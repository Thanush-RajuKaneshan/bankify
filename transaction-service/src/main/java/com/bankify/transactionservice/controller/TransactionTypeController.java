package com.bankify.transactionservice.controller;

import com.bankify.transactionservice.entity.TransactionType;
import com.bankify.transactionservice.repository.TransactionTypeRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Read-only: the built-in types are seeded at startup, and every transaction depends on them. */
@RestController
@RequestMapping("/api/transaction-types")
public class TransactionTypeController {

    private final TransactionTypeRepository types;

    public TransactionTypeController(TransactionTypeRepository types) {
        this.types = types;
    }

    @GetMapping
    public List<TransactionType> getAllTransactionTypes() {
        return types.findAll();
    }
}
