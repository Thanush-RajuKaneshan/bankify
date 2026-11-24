package com.bankify.accountservice.controller;

import com.bankify.accountservice.entity.Account;
import com.bankify.accountservice.service.AccountService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * For transaction-service only. The gateway has no route to /internal, and every request it forwards
 * carries X-User-Id, which this mapping refuses.
 */
@RestController
@RequestMapping(path = "/internal/accounts", headers = "!X-User-Id")
public class InternalAccountController {

    private final AccountService accountService;

    public InternalAccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /** Positive amount credits, negative debits. */
    @PutMapping("/number/{accountNumber}/balance")
    public Account adjustBalance(@PathVariable String accountNumber, @RequestParam BigDecimal amount) {
        return accountService.adjustBalance(accountNumber, amount);
    }
}
