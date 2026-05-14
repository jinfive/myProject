package com.example.myproject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.myproject.domain.batch.SettlementStrategy;
import com.example.myproject.domain.merchant.Merchant;
import com.example.myproject.domain.payment.PaymentStatus;
import com.example.myproject.domain.payment.PaymentType;
import com.example.myproject.domain.settlement.Settlement;
import com.example.myproject.repository.PaymentRepository;
import com.example.myproject.repository.PaymentSettlementAggregation;
import com.example.myproject.repository.SettlementRepository;
import com.example.myproject.service.GroupByBulkSaveSettlementProcessor;
import com.example.myproject.service.SettlementProcessResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class GroupByBulkSaveSettlementProcessorTests {

    private static final LocalDate SETTLEMENT_DATE = LocalDate.of(2026, 5, 8);

    private final PaymentRepository paymentRepository = org.mockito.Mockito.mock(PaymentRepository.class);
    private final SettlementRepository settlementRepository = org.mockito.Mockito.mock(SettlementRepository.class);
    private final GroupByBulkSaveSettlementProcessor processor = new GroupByBulkSaveSettlementProcessor(
            paymentRepository,
            settlementRepository
    );

    @Test
    void groupByBulkSaveUsesAggregationQueryAndSaveAllWithoutLoadingPaymentEntities() {
        Merchant merchant = new Merchant("merchant-a", new BigDecimal("0.0300"));
        PaymentSettlementAggregation aggregation = new PaymentSettlementAggregation(
                merchant,
                merchant.getFeeRate(),
                new BigDecimal("10000.00"),
                new BigDecimal("1000.00"),
                2
        );

        when(settlementRepository.existsBySettlementDateAndProcessingStrategy(
                SETTLEMENT_DATE,
                SettlementStrategy.GROUP_BY_BULK_SAVE
        )).thenReturn(false);
        when(paymentRepository.aggregateCompletedPaymentsByMerchant(
                SETTLEMENT_DATE,
                PaymentStatus.COMPLETED,
                PaymentType.PAYMENT,
                PaymentType.CANCEL
        )).thenReturn(List.of(aggregation));
        when(settlementRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SettlementProcessResult result = processor.run(SETTLEMENT_DATE);

        assertThat(result.processedCount()).isEqualTo(2);
        assertThat(result.settlements()).hasSize(1);
        assertThat(result.settlements().get(0).getProcessingStrategy())
                .isEqualTo(SettlementStrategy.GROUP_BY_BULK_SAVE);
        assertThat(result.settlements().get(0).getFinalSettlementAmount())
                .isEqualByComparingTo(new BigDecimal("8730.00"));

        verify(paymentRepository).aggregateCompletedPaymentsByMerchant(
                SETTLEMENT_DATE,
                PaymentStatus.COMPLETED,
                PaymentType.PAYMENT,
                PaymentType.CANCEL
        );
        verify(paymentRepository, never()).findAllByTransactionDate(any());
        verify(settlementRepository).saveAll(any());
        verify(settlementRepository, never()).save(any(Settlement.class));
        verify(settlementRepository).existsBySettlementDateAndProcessingStrategy(
                eq(SETTLEMENT_DATE),
                eq(SettlementStrategy.GROUP_BY_BULK_SAVE)
        );
    }
}
