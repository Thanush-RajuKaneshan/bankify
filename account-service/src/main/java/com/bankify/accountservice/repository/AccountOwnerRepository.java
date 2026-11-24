package com.bankify.accountservice.repository;

import com.bankify.accountservice.entity.AccountOwner;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountOwnerRepository extends JpaRepository<AccountOwner, Long> {
}
