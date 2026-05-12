package com.example.myproject.repository;

import com.example.myproject.domain.payment.Payment;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    long countByTransactionDate(LocalDate transactionDate);

    boolean existsByTransactionDateNot(LocalDate transactionDate);

    @Query("select min(p.transactionDate) from Payment p")
    LocalDate findMinTransactionDate();

    @Query("select max(p.transactionDate) from Payment p")
    LocalDate findMaxTransactionDate();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Payment p set p.transactionDate = :transactionDate where p.transactionDate <> :transactionDate")
    int updateTransactionDate(LocalDate transactionDate);

    @EntityGraph(attributePaths = "merchant")
    List<Payment> findAllByTransactionDate(LocalDate transactionDate);
}
