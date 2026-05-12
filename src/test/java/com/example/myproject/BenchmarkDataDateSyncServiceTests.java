package com.example.myproject;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.myproject.data.BenchmarkDataDateSyncService;
import com.example.myproject.domain.batch.BatchJobHistory;
import com.example.myproject.domain.batch.BatchJobStatus;
import com.example.myproject.domain.batch.SettlementStrategy;
import com.example.myproject.domain.merchant.Merchant;
import com.example.myproject.domain.payment.Payment;
import com.example.myproject.domain.payment.PaymentStatus;
import com.example.myproject.domain.payment.PaymentType;
import com.example.myproject.domain.settlement.Settlement;
import com.example.myproject.repository.BatchJobHistoryRepository;
import com.example.myproject.repository.MerchantRepository;
import com.example.myproject.repository.PaymentRepository;
import com.example.myproject.repository.SettlementRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BenchmarkDataDateSyncServiceTests {

    private static final LocalDate TARGET_DATE = LocalDate.of(2026, 5, 12);
    private static final LocalDate OLD_DATE = LocalDate.of(2026, 5, 8);

    @Autowired
    private BenchmarkDataDateSyncService benchmarkDataDateSyncService;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private SettlementRepository settlementRepository;

    @Autowired
    private BatchJobHistoryRepository batchJobHistoryRepository;

    @BeforeEach
    void setUp() {
        batchJobHistoryRepository.deleteAll();
        settlementRepository.deleteAll();
        paymentRepository.deleteAll();
        merchantRepository.deleteAll();
    }

    @Test
    void skipsWhenPaymentDataDoesNotExist() {
        BenchmarkDataDateSyncService.SyncResult result = benchmarkDataDateSyncService.syncTo(TARGET_DATE);

        assertThat(result.syncExecuted()).isFalse();
        assertThat(result.paymentExists()).isFalse();
        assertThat(result.updatedPaymentCount()).isZero();
        assertThat(result.deletedSettlementCount()).isZero();
    }

    @Test
    void skipsWhenAllPaymentDatesAlreadyMatchTargetDate() {
        Merchant merchant = merchantRepository.save(new Merchant("merchant-a", new BigDecimal("0.0300")));
        paymentRepository.save(createPayment(merchant, TARGET_DATE));
        settlementRepository.save(createSettlement(merchant, TARGET_DATE));
        batchJobHistoryRepository.save(BatchJobHistory.running(TARGET_DATE, SettlementStrategy.BASIC_LOOP, LocalDateTime.now()));

        BenchmarkDataDateSyncService.SyncResult result = benchmarkDataDateSyncService.syncTo(TARGET_DATE);

        assertThat(result.syncExecuted()).isFalse();
        assertThat(result.paymentExists()).isTrue();
        assertThat(result.updatedPaymentCount()).isZero();
        assertThat(result.deletedSettlementCount()).isZero();
        assertThat(settlementRepository.count()).isEqualTo(1);
        assertThat(batchJobHistoryRepository.count()).isEqualTo(1);
        assertThat(paymentRepository.countByTransactionDate(TARGET_DATE)).isEqualTo(1);
    }

    @Test
    void syncsPaymentDatesAndDeletesSettlementsButKeepsBatchHistories() {
        Merchant merchant = merchantRepository.save(new Merchant("merchant-a", new BigDecimal("0.0300")));
        paymentRepository.save(createPayment(merchant, OLD_DATE));
        paymentRepository.save(createPayment(merchant, TARGET_DATE));
        settlementRepository.save(createSettlement(merchant, OLD_DATE));
        batchJobHistoryRepository.save(new BatchJobHistory(
                OLD_DATE,
                SettlementStrategy.BASIC_LOOP,
                LocalDateTime.now().minusSeconds(1),
                LocalDateTime.now(),
                100,
                2,
                1,
                0,
                BatchJobStatus.SUCCESS,
                null
        ));

        BenchmarkDataDateSyncService.SyncResult result = benchmarkDataDateSyncService.syncTo(TARGET_DATE);

        assertThat(result.syncExecuted()).isTrue();
        assertThat(result.updatedPaymentCount()).isEqualTo(1);
        assertThat(result.deletedSettlementCount()).isEqualTo(1);
        assertThat(settlementRepository.count()).isZero();
        assertThat(batchJobHistoryRepository.count()).isEqualTo(1);
        assertThat(paymentRepository.count()).isEqualTo(2);
        assertThat(paymentRepository.countByTransactionDate(TARGET_DATE)).isEqualTo(2);
        assertThat(paymentRepository.existsByTransactionDateNot(TARGET_DATE)).isFalse();
    }

    private Payment createPayment(Merchant merchant, LocalDate transactionDate) {
        return new Payment(
                merchant,
                PaymentType.PAYMENT,
                PaymentStatus.COMPLETED,
                new BigDecimal("10000.00"),
                transactionDate,
                transactionDate.atStartOfDay()
        );
    }

    private Settlement createSettlement(Merchant merchant, LocalDate settlementDate) {
        return new Settlement(
                merchant,
                settlementDate,
                SettlementStrategy.BASIC_LOOP,
                new BigDecimal("10000.00"),
                BigDecimal.ZERO.setScale(2),
                new BigDecimal("10000.00"),
                merchant.getFeeRate(),
                new BigDecimal("300.00"),
                new BigDecimal("9700.00")
        );
    }
}
