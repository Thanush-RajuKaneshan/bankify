package com.bankify.transactionservice.service;

import com.bankify.transactionservice.entity.Transaction;
import com.bankify.transactionservice.entity.Transaction.Status;
import com.bankify.transactionservice.entity.TransactionType;
import com.bankify.transactionservice.repository.TransactionRepository;
import com.bankify.transactionservice.repository.TransactionTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TransactionService {

    public static final String DEPOSIT = "DEPOSIT";
    public static final String WITHDRAWAL = "WITHDRAWAL";
    public static final String TRANSFER = "TRANSFER";
    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactions;
    private final TransactionTypeRepository types;
    private final AccountClient accounts;

    public TransactionService(TransactionRepository transactions, TransactionTypeRepository types, AccountClient accounts) {
        this.transactions = transactions;
        this.types = types;
        this.accounts = accounts;
    }

    public Transaction deposit(Long userId, String accountNumber, BigDecimal amount) {
        accounts.requireOwner(userId, accountNumber);
        Transaction tx = start(DEPOSIT, amount, null, accountNumber);
        return run(tx, () -> accounts.adjust(accountNumber, amount));
    }

    public Transaction withdraw(Long userId, String accountNumber, BigDecimal amount) {
        accounts.requireOwner(userId, accountNumber);
        Transaction tx = start(WITHDRAWAL, amount, accountNumber, null);
        return run(tx, () -> accounts.adjust(accountNumber, amount.negate()));
    }

    /**
     * Debit then credit. The two accounts may live on different instances, so there is no shared
     * database transaction: if the credit is rejected, the debit is reversed (a compensating action).
     * A credit with an unknown outcome is not refunded, because it may have gone through.
     */
    public Transaction transfer(Long userId, String source, String destination, BigDecimal amount) {
        if (source.equals(destination)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source and destination accounts must be different");
        }
        accounts.requireOwner(userId, source);
        Transaction tx = start(TRANSFER, amount, source, destination);
        return run(tx, () -> {
            accounts.adjust(source, amount.negate());
            try {
                accounts.adjust(destination, amount);
            } catch (ResponseStatusException creditFailed) {
                try {
                    accounts.adjust(source, amount);
                } catch (RuntimeException refundFailed) {
                    log.error("Transfer {}: refund of {} to {} failed; needs manual correction",
                            tx.getTransactionId(), amount, source, refundFailed);
                    tx.setStatus(Status.REFUND_FAILED);
                }
                throw creditFailed;
            }
        });
    }

    /** Same visibility as the history: the sender always, the recipient once the money has arrived. */
    public Transaction getTransaction(Long userId, Long id) {
        return transactions.findById(id).filter(tx -> {
            String source = tx.getSourceAccountNumber();
            String destination = tx.getDestinationAccountNumber();
            return source != null && owns(userId, source)
                    || destination != null && (source == null || tx.getStatus() == Status.COMPLETED) && owns(userId, destination);
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction " + id + " not found"));
    }

    private boolean owns(Long userId, String accountNumber) {
        try {
            accounts.requireOwner(userId, accountNumber);
            return true;
        } catch (ResponseStatusException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return false;
            }
            throw e;
        }
    }

    public List<Transaction> getAccountHistory(Long userId, String accountNumber) {
        accounts.requireOwner(userId, accountNumber);
        return transactions.findHistory(accountNumber);
    }

    private Transaction start(String typeName, BigDecimal amount, String source, String destination) {
        if (amount == null || amount.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be positive");
        }
        TransactionType type = types.findByTypeName(typeName)
                .orElseThrow(() -> new IllegalStateException(typeName + " transaction type is missing"));
        return transactions.save(new Transaction(type, amount, source, destination));
    }

    /** Records the outcome either way, so failed attempts stay visible in the history. */
    private Transaction run(Transaction tx, Runnable moveMoney) {
        try {
            moveMoney.run();
        } catch (ResponseStatusException e) {
            if (tx.getStatus() != Status.REFUND_FAILED) {
                tx.setStatus(Status.FAILED);
            }
            tx.setFailureReason(e.getReason());
            transactions.save(tx);
            throw e;
        } catch (RuntimeException e) {
            // No clear answer from account-service: the money may or may not have moved, so it stays PENDING.
            log.error("Transaction {}: outcome unknown; needs manual check", tx.getTransactionId(), e);
            tx.setStatus(Status.PENDING);
            tx.setFailureReason("Outcome unknown");
            transactions.save(tx);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Outcome of transaction " + tx.getTransactionId() + " is unknown; check its status before retrying");
        }
        tx.setStatus(Status.COMPLETED);
        return transactions.save(tx);
    }
}
