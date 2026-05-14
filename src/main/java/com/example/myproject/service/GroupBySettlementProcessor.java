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
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GroupBySettlementProcessor {

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
        if (settlementRepository.existsBySettlementDateAndProcessingStrategy(settlementDate, strategy)) {
            throw new IllegalStateException("Settlement already exists for date and strategy: "
                    + settlementDate + " / " + strategy);
        }

        List<PaymentSettlementAggregation> aggregations = paymentRepository.aggregateCompletedPaymentsByMerchant(
                settlementDate,
                PaymentStatus.COMPLETED,
                PaymentType.PAYMENT,
                PaymentType.CANCEL
        );

        List<Settlement> settlements = new ArrayList<>(aggregations.size());
        long processedCount = 0;
        for (PaymentSettlementAggregation aggregation : aggregations) {
            processedCount += aggregation.processedCount();

            Settlement settlement = toSettlement(settlementDate, strategy, aggregation);
            settlementRepository.save(settlement);
            afterSettlementSaved(settlement);
            settlements.add(settlement);
        }

        return new SettlementProcessResult(processedCount, settlements);
    }

    protected void afterSettlementSaved(Settlement settlement) {
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
}
