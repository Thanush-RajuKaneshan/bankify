package com.bankify.accountservice.repository;

import com.bankify.accountservice.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByUserId(Long userId);
    Optional<Account> findByAccountNumber(String accountNumber);
    boolean existsByAccountNumber(String accountNumber);

    /**
     * Adds {@code amount} (negative to debit) in one SQL statement, so concurrent updates cannot
     * overwrite each other and a debit can never take the balance below zero.
     * Returns 0 when the account is missing or the funds are insufficient.
     */
    @Modifying(clearAutomatically = true)
    @Query("update Account a set a.balance = a.balance + :amount "
            + "where a.accountNumber = :accountNumber and a.balance + :amount >= 0")
    int adjustBalance(@Param("accountNumber") String accountNumber, @Param("amount") BigDecimal amount);
}
