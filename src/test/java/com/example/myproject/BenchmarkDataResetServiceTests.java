package com.example.myproject;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.myproject.data.BenchmarkDataProperties;
import com.example.myproject.data.DummyDataService;
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
class BenchmarkDataResetServiceTests {

    private static final LocalDate TARGET_DATE = LocalDate.of(2026, 5, 15);
    private static final LocalDate OLD_DATE = LocalDate.of(2026, 5, 8);

    @Autowired
    private DummyDataService dummyDataService;

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
    void regeneratesBenchmarkDataButKeepsBatchHistories() {
        Merchant oldMerchant = merchantRepository.save(new Merchant("old-merchant", new BigDecimal("0.0300")));
        paymentRepository.save(createPayment(oldMerchant, OLD_DATE));
        settlementRepository.save(createSettlement(oldMerchant, OLD_DATE));
        batchJobHistoryRepository.save(new BatchJobHistory(
                OLD_DATE,
                SettlementStrategy.BASIC_LOOP,
                LocalDateTime.now().minusSeconds(1),
                LocalDateTime.now(),
                100,
                1,
                1,
                0,
                BatchJobStatus.SUCCESS,
                null
        ));

        BenchmarkDataProperties properties = new BenchmarkDataProperties();
        properties.setProfile("test-medium");
        properties.setResetEnabled(true);
        properties.setMerchantCount(5);
        properties.setPaymentCount(20);
        properties.setBatchSize(7);
        properties.setTargetDate(TARGET_DATE);

        DummyDataService.BenchmarkGenerationResult result = dummyDataService.regenerateBenchmark(properties);

        assertThat(result.deletedSettlementCount()).isEqualTo(1);
        assertThat(result.deletedPaymentCount()).isEqualTo(1);
        assertThat(result.deletedMerchantCount()).isEqualTo(1);
        assertThat(result.merchantCount()).isEqualTo(5);
        assertThat(result.paymentCount()).isEqualTo(20);
        assertThat(result.targetDatePaymentCount()).isEqualTo(20);
        assertThat(merchantRepository.count()).isEqualTo(5);
        assertThat(paymentRepository.count()).isEqualTo(20);
        assertThat(paymentRepository.countByTransactionDate(TARGET_DATE)).isEqualTo(20);
        assertThat(settlementRepository.count()).isZero();
        assertThat(batchJobHistoryRepository.count()).isEqualTo(1);
    }

    @Test
    void regeneratesDateDistributedBenchmarkData() {
        BenchmarkDataProperties properties = new BenchmarkDataProperties();
        properties.setProfile("test-large-date-distributed");
        properties.setResetEnabled(true);
        properties.setMerchantCount(5);
        properties.setPaymentCount(31);
        properties.setBatchSize(7);
        properties.setTargetDate(LocalDate.of(2026, 5, 15));
        properties.setDateDistributionStart(LocalDate.of(2026, 5, 1));
        properties.setDateDistributionEnd(LocalDate.of(2026, 5, 31));

        DummyDataService.BenchmarkGenerationResult result = dummyDataService.regenerateBenchmark(properties);

        assertThat(result.merchantCount()).isEqualTo(5);
        assertThat(result.paymentCount()).isEqualTo(31);
        assertThat(result.targetDate()).isEqualTo(LocalDate.of(2026, 5, 15));
        assertThat(result.targetDatePaymentCount()).isEqualTo(1);
        assertThat(paymentRepository.countByTransactionDate(LocalDate.of(2026, 5, 1))).isEqualTo(1);
        assertThat(paymentRepository.countByTransactionDate(LocalDate.of(2026, 5, 15))).isEqualTo(1);
        assertThat(paymentRepository.countByTransactionDate(LocalDate.of(2026, 5, 31))).isEqualTo(1);
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
