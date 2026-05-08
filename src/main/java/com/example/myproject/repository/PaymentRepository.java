package com.example.myproject.repository;

import com.example.myproject.domain.payment.Payment;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    long countByTransactionDate(LocalDate transactionDate);
}
