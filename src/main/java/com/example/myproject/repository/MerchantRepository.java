package com.example.myproject.repository;

import com.example.myproject.domain.merchant.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface MerchantRepository extends JpaRepository<Merchant, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Merchant")
    int deleteAllInBulk();
}
