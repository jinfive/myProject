package com.example.myproject.service;

import com.example.myproject.domain.batch.BatchJobHistory;
import com.example.myproject.domain.batch.BatchJobStatus;
import com.example.myproject.domain.batch.SettlementStrategy;
import com.example.myproject.domain.merchant.Merchant;
import com.example.myproject.domain.payment.Payment;
import com.example.myproject.domain.payment.PaymentStatus;
import com.example.myproject.domain.payment.PaymentType;
import com.example.myproject.domain.settlement.Settlement;
import com.example.myproject.repository.BatchJobHistoryRepository;
import com.example.myproject.repository.PaymentRepository;
import com.example.myproject.repository.SettlementRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BasicLoopSettlementService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final PaymentRepository paymentRepository;
    private final SettlementRepository settlementRepository;
    private final BatchJobHistoryRepository batchJobHistoryRepository;

    public BasicLoopSettlementService(
            PaymentRepository paymentRepository,
            SettlementRepository settlementRepository,
            BatchJobHistoryRepository batchJobHistoryRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.settlementRepository = settlementRepository;
        this.batchJobHistoryRepository = batchJobHistoryRepository;
    }

    @Transactional
    public SettlementRunResult run(LocalDate settlementDate, SettlementStrategy strategy) {
        if (strategy != SettlementStrategy.BASIC_LOOP) {
            throw new IllegalArgumentException("Only BASIC_LOOP strategy is supported in this step.");
        }
        if (settlementRepository.existsBySettlementDate(settlementDate)) {
            throw new IllegalStateException("Settlement already exists for date: " + settlementDate);
        }

        LocalDateTime startedAt = LocalDateTime.now();
        List<Payment> payments = paymentRepository.findAllByTransactionDate(settlementDate);
        Map<Long, SettlementAccumulator> accumulatorMap = new LinkedHashMap<>();

        for (Payment payment : payments) {
            if (payment.getStatus() != PaymentStatus.COMPLETED) {
                continue;
            }

            Merchant merchant = payment.getMerchant();
            SettlementAccumulator accumulator = accumulatorMap.computeIfAbsent(
                    merchant.getId(),
                    ignored -> new SettlementAccumulator(merchant)
            );

            if (payment.getType() == PaymentType.PAYMENT) {
                accumulator.addPaymentAmount(payment.getAmount());
            } else if (payment.getType() == PaymentType.CANCEL) {
                accumulator.addCancelAmount(payment.getAmount());
            }
        }

        List<Settlement> settlements = accumulatorMap.values()
                .stream()
                .map(accumulator -> accumulator.toSettlement(settlementDate))
                .toList();

        for (Settlement settlement : settlements) {
            settlementRepository.save(settlement);
        }

        LocalDateTime endedAt = LocalDateTime.now();
        long elapsedMs = Duration.between(startedAt, endedAt).toMillis();

        BatchJobHistory history = batchJobHistoryRepository.save(new BatchJobHistory(
                settlementDate,
                SettlementStrategy.BASIC_LOOP,
                startedAt,
                endedAt,
                elapsedMs,
                payments.size(),
                settlements.size(),
                0,
                BatchJobStatus.SUCCESS
        ));

        return new SettlementRunResult(history, settlements);
    }

    @Transactional(readOnly = true)
    public List<Settlement> getSettlements(LocalDate settlementDate) {
        return settlementRepository.findAllBySettlementDateOrderByFinalSettlementAmountDesc(settlementDate);
    }

    @Transactional(readOnly = true)
    public List<BatchJobHistory> getBatchHistories() {
        return batchJobHistoryRepository.findTop20ByOrderByStartedAtDesc();
    }

    private static class SettlementAccumulator {

        private final Merchant merchant;
        private BigDecimal totalPaymentAmount = ZERO;
        private BigDecimal totalCancelAmount = ZERO;

        private SettlementAccumulator(Merchant merchant) {
            this.merchant = merchant;
        }

        private void addPaymentAmount(BigDecimal amount) {
            totalPaymentAmount = totalPaymentAmount.add(amount);
        }

        private void addCancelAmount(BigDecimal amount) {
            totalCancelAmount = totalCancelAmount.add(amount);
        }

        private Settlement toSettlement(LocalDate settlementDate) {
            BigDecimal netSalesAmount = totalPaymentAmount.subtract(totalCancelAmount);
            BigDecimal feeAmount = netSalesAmount
                    .multiply(merchant.getFeeRate())
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal finalSettlementAmount = netSalesAmount.subtract(feeAmount);

            return new Settlement(
                    merchant,
                    settlementDate,
                    totalPaymentAmount,
                    totalCancelAmount,
                    netSalesAmount,
                    merchant.getFeeRate(),
                    feeAmount,
                    finalSettlementAmount
            );
        }
    }
}
