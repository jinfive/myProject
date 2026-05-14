package com.example.myproject.repository;

import com.example.myproject.domain.payment.Payment;
import com.example.myproject.domain.payment.PaymentStatus;
import com.example.myproject.domain.payment.PaymentType;
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

    @Query("""
            select new com.example.myproject.repository.PaymentSettlementAggregation(
                m,
                m.feeRate,
                coalesce(sum(case when p.type = :paymentType then p.amount else 0 end), 0),
                coalesce(sum(case when p.type = :cancelType then p.amount else 0 end), 0),
                count(p)
            )
            from Payment p
            join p.merchant m
            where p.transactionDate = :transactionDate
              and p.status = :status
            group by m, m.feeRate
            order by m.id
            """)
    List<PaymentSettlementAggregation> aggregateCompletedPaymentsByMerchant(
            LocalDate transactionDate,
            PaymentStatus status,
            PaymentType paymentType,
            PaymentType cancelType
    );
}
