package com.example.myproject.service;

import com.example.myproject.domain.batch.SettlementStrategy;
import com.example.myproject.domain.merchant.Merchant;
import com.example.myproject.domain.payment.PaymentStatus;
import com.example.myproject.domain.payment.PaymentType;
import com.example.myproject.domain.settlement.Settlement;
import com.example.myproject.repository.PaymentRepository;
import com.example.myproject.repository.PaymentSettlementAggregation;
import com.example.myproject.repository.SettlementRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GroupByBulkSaveSettlementProcessor {

    private static final Logger log = LoggerFactory.getLogger(GroupByBulkSaveSettlementProcessor.class);

    private final PaymentRepository paymentRepository;
    private final SettlementRepository settlementRepository;

    public GroupByBulkSaveSettlementProcessor(
            PaymentRepository paymentRepository,
            SettlementRepository settlementRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.settlementRepository = settlementRepository;
    }

    @Transactional
    public SettlementProcessResult run(LocalDate settlementDate) {
        SettlementStrategy strategy = SettlementStrategy.GROUP_BY_BULK_SAVE;
        long duplicateCheckStartedAt = System.nanoTime();
        if (settlementRepository.existsBySettlementDateAndProcessingStrategy(settlementDate, strategy)) {
            throw new IllegalStateException("Settlement already exists for date and strategy: "
                    + settlementDate + " / " + strategy);
        }
        long duplicateCheckElapsedNanos = System.nanoTime() - duplicateCheckStartedAt;

        long aggregationStartedAt = System.nanoTime();
        List<PaymentSettlementAggregation> aggregations = paymentRepository.aggregateCompletedPaymentsByMerchant(
                settlementDate,
                PaymentStatus.COMPLETED,
                PaymentType.PAYMENT,
                PaymentType.CANCEL
        );
        long aggregationElapsedNanos = System.nanoTime() - aggregationStartedAt;

        long processedCount = aggregations.stream()
                .mapToLong(PaymentSettlementAggregation::processedCount)
                .sum();
        long objectCreationStartedAt = System.nanoTime();
        List<Settlement> settlements = aggregations.stream()
                .map(aggregation -> toSettlement(settlementDate, strategy, aggregation))
                .toList();
        long objectCreationElapsedNanos = System.nanoTime() - objectCreationStartedAt;

        long saveStartedAt = System.nanoTime();
        List<Settlement> savedSettlements = settlementRepository.saveAll(settlements);
        long saveElapsedNanos = System.nanoTime() - saveStartedAt;
        afterSettlementsSaved(savedSettlements);

        log.info(
                "settlement timing date={} strategy={} section=processor duplicate_check_ms={} aggregation_ms={} object_creation_ms={} save_ms={} processed_count={} settlement_count={}",
                settlementDate,
                strategy,
                toMillis(duplicateCheckElapsedNanos),
                toMillis(aggregationElapsedNanos),
                toMillis(objectCreationElapsedNanos),
                toMillis(saveElapsedNanos),
                processedCount,
                savedSettlements.size()
        );
        return new SettlementProcessResult(processedCount, savedSettlements);
    }

    protected void afterSettlementsSaved(List<Settlement> settlements) {
    }

    private Settlement toSettlement(
            LocalDate settlementDate,
            SettlementStrategy strategy,
            PaymentSettlementAggregation aggregation
    ) {
        Merchant merchant = aggregation.merchant();
        BigDecimal totalPaymentAmount = aggregation.totalPaymentAmount();
        BigDecimal totalCancelAmount = aggregation.totalCancelAmount();
        BigDecimal netSalesAmount = totalPaymentAmount.subtract(totalCancelAmount);
        BigDecimal feeAmount = netSalesAmount
                .multiply(aggregation.feeRate())
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal finalSettlementAmount = netSalesAmount.subtract(feeAmount);

        return new Settlement(
                merchant,
                settlementDate,
                strategy,
                totalPaymentAmount,
                totalCancelAmount,
                netSalesAmount,
                aggregation.feeRate(),
                feeAmount,
                finalSettlementAmount
        );
    }

    private double toMillis(long nanos) {
        return nanos / 1_000_000.0;
    }
}
