package com.bankify.accountservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Local copy of a registered user, kept in sync from user-service's Kafka events. */
@Entity
@Table(name = "account_owners")
public class AccountOwner {
    @Id
    private Long userId;
    private String username;

    public AccountOwner() {
    }

    public AccountOwner(Long userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
}
