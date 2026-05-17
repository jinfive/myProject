package com.example.myproject.service;

import com.example.myproject.domain.batch.SettlementStrategy;
import com.example.myproject.domain.merchant.Merchant;
import com.example.myproject.domain.payment.PaymentStatus;
import com.example.myproject.domain.payment.PaymentType;
import com.example.myproject.domain.settlement.Settlement;
import com.example.myproject.repository.PaymentRepository;
import com.example.myproject.repository.PaymentSettlementAggregation;
import com.example.myproject.repository.PaymentSettlementAggregationProjection;
import com.example.myproject.repository.SettlementRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GroupBySettlementProcessor {

    private static final Logger log = LoggerFactory.getLogger(GroupBySettlementProcessor.class);

    private final PaymentRepository paymentRepository;
    private final SettlementRepository settlementRepository;

    public GroupBySettlementProcessor(
            PaymentRepository paymentRepository,
            SettlementRepository settlementRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.settlementRepository = settlementRepository;
    }

    @Transactional
    public SettlementProcessResult run(LocalDate settlementDate) {
        SettlementStrategy strategy = SettlementStrategy.GROUP_BY_QUERY;
        long duplicateCheckStartedAt = System.nanoTime();
        if (settlementRepository.existsBySettlementDateAndProcessingStrategy(settlementDate, strategy)) {
            throw new IllegalStateException("Settlement already exists for date and strategy: "
                    + settlementDate + " / " + strategy);
        }
        long duplicateCheckElapsedNanos = System.nanoTime() - duplicateCheckStartedAt;

        long aggregationStartedAt = System.nanoTime();
        List<PaymentSettlementAggregationProjection> aggregationProjections =
                paymentRepository.aggregateCompletedPaymentsByMerchant(
                settlementDate,
                PaymentStatus.COMPLETED.name(),
                PaymentType.PAYMENT.name(),
                PaymentType.CANCEL.name()
        );
        List<PaymentSettlementAggregation> aggregations = aggregationProjections.stream()
                .map(this::toAggregation)
                .toList();
        long aggregationElapsedNanos = System.nanoTime() - aggregationStartedAt;

        List<Settlement> settlements = new ArrayList<>(aggregations.size());
        long processedCount = 0;
        long objectCreationElapsedNanos = 0;
        long saveElapsedNanos = 0;
        for (PaymentSettlementAggregation aggregation : aggregations) {
            processedCount += aggregation.processedCount();

            long objectCreationStartedAt = System.nanoTime();
            Settlement settlement = toSettlement(settlementDate, strategy, aggregation);
            objectCreationElapsedNanos += System.nanoTime() - objectCreationStartedAt;
            long saveStartedAt = System.nanoTime();
            settlementRepository.save(settlement);
            saveElapsedNanos += System.nanoTime() - saveStartedAt;
            afterSettlementSaved(settlement);
            settlements.add(settlement);
        }

        log.info(
                "settlement timing date={} strategy={} section=processor duplicate_check_ms={} aggregation_ms={} object_creation_ms={} save_ms={} processed_count={} settlement_count={}",
                settlementDate,
                strategy,
                toMillis(duplicateCheckElapsedNanos),
                toMillis(aggregationElapsedNanos),
                toMillis(objectCreationElapsedNanos),
                toMillis(saveElapsedNanos),
                processedCount,
                settlements.size()
        );
        return new SettlementProcessResult(processedCount, settlements);
    }

    protected void afterSettlementSaved(Settlement settlement) {
    }

    private PaymentSettlementAggregation toAggregation(PaymentSettlementAggregationProjection projection) {
        Merchant merchant = new Merchant(projection.getMerchantId(), projection.getMerchantName(), projection.getFeeRate());
        return new PaymentSettlementAggregation(
                merchant,
                projection.getFeeRate(),
                projection.getPaymentAmount(),
                projection.getCancelAmount(),
                projection.getProcessedCount()
        );
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
