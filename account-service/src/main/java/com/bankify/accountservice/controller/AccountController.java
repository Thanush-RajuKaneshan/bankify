package com.bankify.accountservice.controller;

import com.bankify.accountservice.entity.Account;
import com.bankify.accountservice.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** The caller is always the user in X-User-Id, which the gateway sets from the verified JWT. */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Account createAccount(@RequestHeader("X-User-Id") Long userId) {
        return accountService.createAccount(userId);
    }

    @GetMapping
    public List<Account> getMyAccounts(@RequestHeader("X-User-Id") Long userId) {
        return accountService.getAccountsByUserId(userId);
    }

    @GetMapping("/{id}")
    public Account getAccountById(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id) {
        return accountService.getOwnAccount(userId, id);
    }

    @GetMapping("/number/{accountNumber}")
    public Account getAccountByAccountNumber(@RequestHeader("X-User-Id") Long userId, @PathVariable String accountNumber) {
        return accountService.getOwnAccount(userId, accountNumber);
    }
}
