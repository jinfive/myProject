package com.example.myproject.service;

import com.example.myproject.domain.batch.SettlementStrategy;
import com.example.myproject.domain.merchant.Merchant;
import com.example.myproject.domain.payment.Payment;
import com.example.myproject.domain.payment.PaymentStatus;
import com.example.myproject.domain.payment.PaymentType;
import com.example.myproject.domain.settlement.Settlement;
import com.example.myproject.repository.PaymentRepository;
import com.example.myproject.repository.SettlementRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BasicLoopSettlementProcessor {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final PaymentRepository paymentRepository;
    private final SettlementRepository settlementRepository;

    public BasicLoopSettlementProcessor(
            PaymentRepository paymentRepository,
            SettlementRepository settlementRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.settlementRepository = settlementRepository;
    }

    @Transactional
    public SettlementProcessResult run(LocalDate settlementDate) {
        SettlementStrategy strategy = SettlementStrategy.BASIC_LOOP;
        if (settlementRepository.existsBySettlementDateAndProcessingStrategy(settlementDate, strategy)) {
            throw new IllegalStateException("Settlement already exists for date and strategy: "
                    + settlementDate + " / " + strategy);
        }

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
                .map(accumulator -> accumulator.toSettlement(settlementDate, strategy))
                .toList();

        for (Settlement settlement : settlements) {
            settlementRepository.save(settlement);
            afterSettlementSaved(settlement);
        }

        return new SettlementProcessResult(payments.size(), settlements);
    }

    protected void afterSettlementSaved(Settlement settlement) {
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

        private Settlement toSettlement(LocalDate settlementDate, SettlementStrategy strategy) {
            BigDecimal netSalesAmount = totalPaymentAmount.subtract(totalCancelAmount);
            BigDecimal feeAmount = netSalesAmount
                    .multiply(merchant.getFeeRate())
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal finalSettlementAmount = netSalesAmount.subtract(feeAmount);

            return new Settlement(
                    merchant,
                    settlementDate,
                    strategy,
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
