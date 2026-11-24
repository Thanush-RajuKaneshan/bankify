package com.bankify.transactionservice.controller;

import com.bankify.transactionservice.entity.Transaction;
import com.bankify.transactionservice.service.TransactionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    public record Movement(@NotBlank String accountNumber,
                           @NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal amount) {
    }

    public record Transfer(@NotBlank String sourceAccountNumber, @NotBlank String destinationAccountNumber,
                           @NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal amount) {
    }

    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    @PostMapping("/deposit")
    public Transaction deposit(@RequestHeader("X-User-Id") Long userId, @Valid @RequestBody Movement request) {
        return service.deposit(userId, request.accountNumber(), request.amount());
    }

    @PostMapping("/withdraw")
    public Transaction withdraw(@RequestHeader("X-User-Id") Long userId, @Valid @RequestBody Movement request) {
        return service.withdraw(userId, request.accountNumber(), request.amount());
    }

    @PostMapping("/transfer")
    public Transaction transfer(@RequestHeader("X-User-Id") Long userId, @Valid @RequestBody Transfer request) {
        return service.transfer(userId, request.sourceAccountNumber(), request.destinationAccountNumber(), request.amount());
    }

    @GetMapping("/{id}")
    public Transaction getTransaction(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id) {
        return service.getTransaction(userId, id);
    }

    @GetMapping("/account/{accountNumber}")
    public List<Transaction> getAccountHistory(@RequestHeader("X-User-Id") Long userId, @PathVariable String accountNumber) {
        return service.getAccountHistory(userId, accountNumber);
    }
}
