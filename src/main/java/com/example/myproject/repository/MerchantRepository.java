package com.example.myproject.repository;

import com.example.myproject.domain.merchant.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantRepository extends JpaRepository<Merchant, Long> {
}
