package com.bankify.accountservice;

import com.bankify.accountservice.entity.Account;
import com.bankify.accountservice.events.UserEventsListener;
import com.bankify.accountservice.repository.AccountOwnerRepository;
import com.bankify.accountservice.repository.AccountRepository;
import com.bankify.accountservice.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class AccountServiceTest {

    @Autowired AccountService service;
    @Autowired UserEventsListener listener;
    @Autowired AccountRepository accounts;
    @Autowired AccountOwnerRepository owners;

    @Test
    void userEventFromKafkaLetsThatUserOpenAccounts() throws Exception {
        assertThatThrownBy(() -> service.createAccount(42L))
                .hasMessageContaining("Unknown user 42");

        listener.onUserEvent("{\"type\":\"USER_REGISTERED\",\"userId\":42,\"username\":\"asha\"}");
        Account account = service.createAccount(42L);

        assertThat(owners.existsById(42L)).isTrue();
        assertThat(account.getAccountNumber()).matches("BNK\\d{10}");
        assertThat(account.getBalance()).isEqualByComparingTo("0");
    }

    @Test
    void overdraftIsAConflictAndMissingAccountIsNotFound() throws Exception {
        listener.onUserEvent("{\"type\":\"USER_REGISTERED\",\"userId\":7,\"username\":\"ravi\"}");
        String number = service.createAccount(7L).getAccountNumber();
        service.adjustBalance(number, new BigDecimal("5.00"));

        assertThatThrownBy(() -> service.adjustBalance(number, new BigDecimal("-6")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        assertThatThrownBy(() -> service.adjustBalance("BNK0000000000", BigDecimal.ONE))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThat(service.adjustBalance(number, new BigDecimal("-5")).getBalance()).isEqualByComparingTo("0");
    }

    @Test
    void otherUsersAccountsLookMissing() throws Exception {
        listener.onUserEvent("{\"type\":\"USER_REGISTERED\",\"userId\":8,\"username\":\"meera\"}");
        Account account = service.createAccount(8L);

        assertThat(service.getOwnAccount(8L, account.getAccountNumber()).getUserId()).isEqualTo(8L);
        assertThatThrownBy(() -> service.getOwnAccount(9L, account.getAccountNumber())).hasMessageContaining("not found");
        assertThatThrownBy(() -> service.getOwnAccount(9L, account.getAccountId())).hasMessageContaining("not found");
    }
}
