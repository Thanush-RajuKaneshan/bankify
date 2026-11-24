package com.bankify.transactionservice.repository;

import com.bankify.transactionservice.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /** Incoming transfers appear only once completed; a sender's failed attempts are not the recipient's business. */
    @Query("select t from Transaction t where t.sourceAccountNumber = :account "
            + "or (t.destinationAccountNumber = :account and (t.sourceAccountNumber is null or t.status = COMPLETED)) "
            + "order by t.createdAt desc")
    List<Transaction> findHistory(@Param("account") String account);
}
