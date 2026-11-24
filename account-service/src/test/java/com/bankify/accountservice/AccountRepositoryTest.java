package com.bankify.accountservice;

import com.bankify.accountservice.entity.Account;
import com.bankify.accountservice.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** Runs the real balance-update SQL against an in-memory database. */
@DataJpaTest
class AccountRepositoryTest {

    @Autowired
    AccountRepository accounts;

    @Test
    void creditsAndDebitsInOneStatement() {
        accounts.save(new Account("BNK1", new BigDecimal("100.00"), 1L));

        assertThat(accounts.adjustBalance("BNK1", new BigDecimal("50.25"))).isEqualTo(1);
        assertThat(accounts.adjustBalance("BNK1", new BigDecimal("-150.25"))).isEqualTo(1);
        assertThat(accounts.findByAccountNumber("BNK1").orElseThrow().getBalance()).isEqualByComparingTo("0");
    }

    @Test
    void refusesToOverdraw() {
        accounts.save(new Account("BNK2", new BigDecimal("10.00"), 1L));

        assertThat(accounts.adjustBalance("BNK2", new BigDecimal("-10.01"))).isZero();
        assertThat(accounts.findByAccountNumber("BNK2").orElseThrow().getBalance()).isEqualByComparingTo("10.00");
    }

    @Test
    void reportsMissingAccount() {
        assertThat(accounts.adjustBalance("NOPE", BigDecimal.ONE)).isZero();
    }
}
