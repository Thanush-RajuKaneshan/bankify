package com.bankify.transactionservice;

import com.bankify.transactionservice.entity.Transaction;
import com.bankify.transactionservice.entity.Transaction.Status;
import com.bankify.transactionservice.repository.TransactionRepository;
import com.bankify.transactionservice.service.AccountClient;
import com.bankify.transactionservice.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
class TransactionServiceTest {

    @Autowired TransactionService service;
    @Autowired TransactionRepository transactions;
    @MockitoBean AccountClient accounts;

    private static final BigDecimal AMOUNT = new BigDecimal("40.00");

    @Test
    void transferDebitsThenCredits() {
        Transaction tx = service.transfer(1L, "A", "B", AMOUNT);

        verify(accounts).adjust("A", AMOUNT.negate());
        verify(accounts).adjust("B", AMOUNT);
        assertThat(tx.getStatus()).isEqualTo(Status.COMPLETED);
    }

    @Test
    void failedCreditRefundsTheSource() {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Account B not found"))
                .when(accounts).adjust(eq("B"), any());

        assertThatThrownBy(() -> service.transfer(1L, "A", "B", AMOUNT)).hasMessageContaining("Account B not found");

        verify(accounts).adjust("A", AMOUNT.negate());
        verify(accounts).adjust("A", AMOUNT); // compensating refund
        Transaction saved = transactions.findAll().getLast();
        assertThat(saved.getStatus()).isEqualTo(Status.FAILED);
        assertThat(saved.getFailureReason()).isEqualTo("Account B not found");
    }

    @Test
    void failedRefundIsFlaggedForManualFix() {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND)).when(accounts).adjust(eq("B"), any());
        doNothing().doThrow(new RuntimeException("account-service down")).when(accounts).adjust(eq("A"), any());

        assertThatThrownBy(() -> service.transfer(1L, "A", "B", AMOUNT));

        assertThat(transactions.findAll().getLast().getStatus()).isEqualTo(Status.REFUND_FAILED);
    }

    @Test
    void insufficientFundsLeavesNoMoneyMoved() {
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient funds"))
                .when(accounts).adjust("A", AMOUNT.negate());

        assertThatThrownBy(() -> service.withdraw(1L, "A", AMOUNT)).hasMessageContaining("Insufficient funds");
        assertThatThrownBy(() -> service.transfer(1L, "A", "B", AMOUNT)).hasMessageContaining("Insufficient funds");

        verify(accounts, never()).adjust(eq("B"), any());
    }

    @Test
    void rejectsNonPositiveAmountsAndSelfTransfers() {
        assertThatThrownBy(() -> service.deposit(1L, "A", BigDecimal.ZERO)).hasMessageContaining("must be positive");
        assertThatThrownBy(() -> service.transfer(1L, "A", "A", AMOUNT)).hasMessageContaining("must be different");
        verify(accounts, never()).adjust(any(), any());
    }

    @Test
    void onlyTheOwnerCanMoveMoneyOrSeeHistory() {
        Transaction tx = service.transfer(1L, "A", "B", AMOUNT);
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Account A not found"))
                .when(accounts).requireOwner(eq(2L), any());
        clearInvocations(accounts);

        assertThatThrownBy(() -> service.deposit(2L, "A", AMOUNT)).hasMessageContaining("not found");
        assertThatThrownBy(() -> service.withdraw(2L, "A", AMOUNT)).hasMessageContaining("not found");
        assertThatThrownBy(() -> service.transfer(2L, "A", "B", AMOUNT)).hasMessageContaining("not found");
        assertThatThrownBy(() -> service.getAccountHistory(2L, "A")).hasMessageContaining("not found");
        assertThatThrownBy(() -> service.getTransaction(2L, tx.getTransactionId())).hasMessageContaining("not found");
        verify(accounts, never()).adjust(any(), any());
    }

    @Test
    void recipientDoesNotSeeFailedIncomingTransfers() {
        service.transfer(1L, "S", "R", AMOUNT);
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient funds")).when(accounts).adjust(eq("S"), any());
        assertThatThrownBy(() -> service.transfer(1L, "S", "R", AMOUNT));

        assertThat(service.getAccountHistory(1L, "S")).hasSize(2);
        assertThat(service.getAccountHistory(1L, "R")).hasSize(1);
    }

    @Test
    void creditWithUnknownOutcomeIsNotRefunded() {
        doThrow(new ResourceAccessException("timed out")).when(accounts).adjust(eq("B"), any());

        assertThatThrownBy(() -> service.transfer(1L, "A", "B", AMOUNT)).hasMessageContaining("unknown");

        verify(accounts, never()).adjust("A", AMOUNT);
        assertThat(transactions.findAll().getLast().getStatus()).isEqualTo(Status.PENDING);
    }
}
