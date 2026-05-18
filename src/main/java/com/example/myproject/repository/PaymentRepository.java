package com.example.myproject.repository;

import com.example.myproject.domain.payment.Payment;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Payment")
    int deleteAllInBulk();

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

    @Query(value = """
            select
                agg.merchant_id as merchantId,
                m.name as merchantName,
                m.fee_rate as feeRate,
                agg.payment_amount as paymentAmount,
                agg.cancel_amount as cancelAmount,
                agg.processed_count as processedCount
            from (
                select
                    p.merchant_id,
                    coalesce(sum(case when p.type = :paymentType then p.amount else 0 end), 0) as payment_amount,
                    coalesce(sum(case when p.type = :cancelType then p.amount else 0 end), 0) as cancel_amount,
                    count(*) as processed_count
                from payments p
                where p.transaction_date = :transactionDate
                  and p.status = :status
                group by p.merchant_id
            ) agg
            join merchants m on m.id = agg.merchant_id
            order by agg.merchant_id
            """, nativeQuery = true)
    List<PaymentSettlementAggregationProjection> aggregateCompletedPaymentsByMerchant(
            LocalDate transactionDate,
            String status,
            String paymentType,
            String cancelType
    );
}
