package com.bankify.accountservice.service;

import com.bankify.accountservice.entity.Account;
import com.bankify.accountservice.repository.AccountOwnerRepository;
import com.bankify.accountservice.repository.AccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;

@Service
public class AccountService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountRepository accounts;
    private final AccountOwnerRepository owners;

    public AccountService(AccountRepository accounts, AccountOwnerRepository owners) {
        this.accounts = accounts;
        this.owners = owners;
    }

    /** Accounts open empty: money only arrives through transaction-service, so the history explains every balance. */
    public Account createAccount(Long userId) {
        if (!owners.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown user " + userId);
        }
        return accounts.save(new Account(newAccountNumber(), BigDecimal.ZERO, userId));
    }

    /** Someone else's account looks the same as a missing one, so account numbers cannot be probed. */
    public Account getOwnAccount(Long userId, Long id) {
        return accounts.findById(id).filter(a -> a.getUserId().equals(userId)).orElseThrow(() -> notFound("id " + id));
    }

    public Account getOwnAccount(Long userId, String accountNumber) {
        return accounts.findByAccountNumber(accountNumber).filter(a -> a.getUserId().equals(userId))
                .orElseThrow(() -> notFound(accountNumber));
    }

    public List<Account> getAccountsByUserId(Long userId) {
        return accounts.findByUserId(userId);
    }

    /** Credits (positive) or debits (negative) an account atomically; 409 if it would overdraw. */
    @Transactional
    public Account adjustBalance(String accountNumber, BigDecimal amount) {
        if (accounts.adjustBalance(accountNumber, amount) == 0) {
            if (!accounts.existsByAccountNumber(accountNumber)) {
                throw notFound(accountNumber);
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient funds in account " + accountNumber);
        }
        return accounts.findByAccountNumber(accountNumber).orElseThrow(() -> notFound(accountNumber));
    }

    private String newAccountNumber() {
        String number;
        do {
            number = "BNK" + String.format("%010d", RANDOM.nextLong(10_000_000_000L));
        } while (accounts.existsByAccountNumber(number));
        return number;
    }

    private static ResponseStatusException notFound(String which) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Account " + which + " not found");
    }
}
