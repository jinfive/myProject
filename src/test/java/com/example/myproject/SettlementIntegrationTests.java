package com.example.myproject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import com.example.myproject.repository.PaymentSettlementAggregationProjection;
import com.example.myproject.repository.SettlementRepository;
import com.example.myproject.service.BasicLoopSettlementService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

@SpringBootTest
class SettlementIntegrationTests {

    private static final LocalDate SETTLEMENT_DATE = LocalDate.of(2026, 5, 8);

    @Autowired
    private BasicLoopSettlementService settlementService;

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
    void groupByQueryProducesSameAmountsAsBasicLoopForSameDate() {
        createMultiMerchantPaymentFixture();

        settlementService.run(SETTLEMENT_DATE, SettlementStrategy.BASIC_LOOP);
        settlementService.run(SETTLEMENT_DATE, SettlementStrategy.GROUP_BY_QUERY);

        assertSameSettlementAmounts(SettlementStrategy.BASIC_LOOP, SettlementStrategy.GROUP_BY_QUERY);
    }

    @Test
    void groupByBulkSaveProducesSameAmountsAsBasicLoopAndGroupByQueryForSameDate() {
        createMultiMerchantPaymentFixture();

        settlementService.run(SETTLEMENT_DATE, SettlementStrategy.BASIC_LOOP);
        settlementService.run(SETTLEMENT_DATE, SettlementStrategy.GROUP_BY_QUERY);
        settlementService.run(SETTLEMENT_DATE, SettlementStrategy.GROUP_BY_BULK_SAVE);

        assertSameSettlementAmounts(SettlementStrategy.BASIC_LOOP, SettlementStrategy.GROUP_BY_BULK_SAVE);
        assertSameSettlementAmounts(SettlementStrategy.GROUP_BY_QUERY, SettlementStrategy.GROUP_BY_BULK_SAVE);
    }

    @Test
    void sameDateAndGroupByQueryCannotRunTwice() {
        createPaymentFixture();

        settlementService.run(SETTLEMENT_DATE, SettlementStrategy.GROUP_BY_QUERY);

        assertThatThrownBy(() -> settlementService.run(SETTLEMENT_DATE, SettlementStrategy.GROUP_BY_QUERY))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Settlement already exists");

        assertThat(settlementRepository.count()).isEqualTo(1);
        assertThat(batchJobHistoryRepository.findAll())
                .extracting(BatchJobHistory::getStatus)
                .containsExactlyInAnyOrder(BatchJobStatus.SUCCESS, BatchJobStatus.FAILED);
    }

    @Test
    void sameDateAndGroupByBulkSaveCannotRunTwice() {
        createPaymentFixture();

        settlementService.run(SETTLEMENT_DATE, SettlementStrategy.GROUP_BY_BULK_SAVE);

        assertThatThrownBy(() -> settlementService.run(SETTLEMENT_DATE, SettlementStrategy.GROUP_BY_BULK_SAVE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Settlement already exists");

        assertThat(settlementRepository.count()).isEqualTo(1);
        assertThat(batchJobHistoryRepository.findAll())
                .extracting(BatchJobHistory::getStatus)
                .containsExactlyInAnyOrder(BatchJobStatus.SUCCESS, BatchJobStatus.FAILED);
    }

    @Test
    void groupByQuerySuccessHistoryContainsStrategyElapsedMsAndCounts() {
        createPaymentFixture();

        settlementService.run(SETTLEMENT_DATE, SettlementStrategy.GROUP_BY_QUERY);

        BatchJobHistory history = batchJobHistoryRepository.findAll().get(0);
        assertThat(history.getStatus()).isEqualTo(BatchJobStatus.SUCCESS);
        assertThat(history.getStrategy()).isEqualTo(SettlementStrategy.GROUP_BY_QUERY);
        assertThat(history.getElapsedMs()).isGreaterThanOrEqualTo(0);
        assertThat(history.getProcessedCount()).isEqualTo(2);
        assertThat(history.getSuccessCount()).isEqualTo(1);
        assertThat(history.getFailureCount()).isZero();
        assertThat(history.getErrorMessage()).isNull();
    }

    @Test
    void groupByBulkSaveStoresSameCountAsAggregationsAndRecordsSuccessHistory() {
        createMultiMerchantPaymentFixture();
        List<PaymentSettlementAggregationProjection> aggregations = paymentRepository.aggregateCompletedPaymentsByMerchant(
                SETTLEMENT_DATE,
                PaymentStatus.COMPLETED.name(),
                PaymentType.PAYMENT.name(),
                PaymentType.CANCEL.name()
        );
        long aggregationProcessedCount = aggregations.stream()
                .mapToLong(PaymentSettlementAggregationProjection::getProcessedCount)
                .sum();

        settlementService.run(SETTLEMENT_DATE, SettlementStrategy.GROUP_BY_BULK_SAVE);

        long savedCount = settlementRepository.countBySettlementDateAndProcessingStrategy(
                SETTLEMENT_DATE,
                SettlementStrategy.GROUP_BY_BULK_SAVE
        );
        BatchJobHistory history = batchJobHistoryRepository.findAll().get(0);

        assertThat(aggregations).hasSize(2);
        assertThat(savedCount).isEqualTo(aggregations.size());
        assertThat(history.getStatus()).isEqualTo(BatchJobStatus.SUCCESS);
        assertThat(history.getStrategy()).isEqualTo(SettlementStrategy.GROUP_BY_BULK_SAVE);
        assertThat(history.getElapsedMs()).isGreaterThanOrEqualTo(0);
        assertThat(history.getProcessedCount()).isEqualTo(aggregationProcessedCount);
        assertThat(history.getSuccessCount()).isEqualTo(aggregations.size());
        assertThat(history.getFailureCount()).isZero();
        assertThat(history.getErrorMessage()).isNull();
    }

    @Test
    void groupByQueryWithNoCompletedPaymentsReturnsEmptySuccessResult() {
        Merchant merchant = merchantRepository.save(new Merchant("merchant-a", new BigDecimal("0.0300")));
        paymentRepository.save(new Payment(
                merchant,
                PaymentType.PAYMENT,
                PaymentStatus.FAILED,
                new BigDecimal("10000.00"),
                SETTLEMENT_DATE,
                LocalDateTime.now()
        ));

        settlementService.run(SETTLEMENT_DATE, SettlementStrategy.GROUP_BY_QUERY);

        BatchJobHistory history = batchJobHistoryRepository.findAll().get(0);
        assertThat(settlementRepository.count()).isZero();
        assertThat(history.getStatus()).isEqualTo(BatchJobStatus.SUCCESS);
        assertThat(history.getStrategy()).isEqualTo(SettlementStrategy.GROUP_BY_QUERY);
        assertThat(history.getProcessedCount()).isZero();
        assertThat(history.getSuccessCount()).isZero();
    }

    @Test
    void groupByBulkSaveWithNoCompletedPaymentsReturnsEmptySuccessResult() {
        Merchant merchant = merchantRepository.save(new Merchant("merchant-a", new BigDecimal("0.0300")));
        paymentRepository.save(new Payment(
                merchant,
                PaymentType.PAYMENT,
                PaymentStatus.FAILED,
                new BigDecimal("10000.00"),
                SETTLEMENT_DATE,
                LocalDateTime.now()
        ));

        settlementService.run(SETTLEMENT_DATE, SettlementStrategy.GROUP_BY_BULK_SAVE);

        BatchJobHistory history = batchJobHistoryRepository.findAll().get(0);
        assertThat(settlementRepository.count()).isZero();
        assertThat(history.getStatus()).isEqualTo(BatchJobStatus.SUCCESS);
        assertThat(history.getStrategy()).isEqualTo(SettlementStrategy.GROUP_BY_BULK_SAVE);
        assertThat(history.getProcessedCount()).isZero();
        assertThat(history.getSuccessCount()).isZero();
    }

    @Test
    void sameDateAndSameStrategyCannotRunTwice() {
        createPaymentFixture();

        settlementService.run(SETTLEMENT_DATE, SettlementStrategy.BASIC_LOOP);

        assertThatThrownBy(() -> settlementService.run(SETTLEMENT_DATE, SettlementStrategy.BASIC_LOOP))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Settlement already exists");

        assertThat(settlementRepository.count()).isEqualTo(1);
        assertThat(batchJobHistoryRepository.findAll())
                .extracting(BatchJobHistory::getStatus)
                .containsExactlyInAnyOrder(BatchJobStatus.SUCCESS, BatchJobStatus.FAILED);
    }

    @Test
    void sameMerchantAndDateCanStoreDifferentStrategiesButNotDuplicateStrategy() {
        Merchant merchant = merchantRepository.save(new Merchant("merchant-a", new BigDecimal("0.0300")));

        for (SettlementStrategy strategy : SettlementStrategy.values()) {
            settlementRepository.saveAndFlush(createSettlement(merchant, SETTLEMENT_DATE, strategy));
        }

        assertThat(settlementRepository.count()).isEqualTo(4);
        assertThatThrownBy(() -> settlementRepository.saveAndFlush(
                createSettlement(merchant, SETTLEMENT_DATE, SettlementStrategy.BASIC_LOOP)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void successHistoryContainsStrategyElapsedMsAndCounts() {
        createPaymentFixture();

        settlementService.run(SETTLEMENT_DATE, SettlementStrategy.BASIC_LOOP);

        BatchJobHistory history = batchJobHistoryRepository.findAll().get(0);
        assertThat(history.getStatus()).isEqualTo(BatchJobStatus.SUCCESS);
        assertThat(history.getStrategy()).isEqualTo(SettlementStrategy.BASIC_LOOP);
        assertThat(history.getElapsedMs()).isGreaterThanOrEqualTo(0);
        assertThat(history.getProcessedCount()).isEqualTo(2);
        assertThat(history.getSuccessCount()).isEqualTo(1);
        assertThat(history.getFailureCount()).isZero();
        assertThat(history.getErrorMessage()).isNull();
    }

    @Test
    void resetSettlementsDeletesOnlySettlementResultsForDateAndKeepsHistories() {
        createPaymentFixture();
        settlementService.run(SETTLEMENT_DATE, SettlementStrategy.BASIC_LOOP);
        settlementService.run(SETTLEMENT_DATE, SettlementStrategy.GROUP_BY_QUERY);

        int deletedCount = settlementService.resetSettlements(SETTLEMENT_DATE);

        assertThat(deletedCount).isEqualTo(2);
        assertThat(settlementRepository.count()).isZero();
        assertThat(batchJobHistoryRepository.findAll()).hasSize(2);
    }

    private void assertSameSettlementAmounts(SettlementStrategy baselineStrategy, SettlementStrategy targetStrategy) {
        List<Settlement> baselineSettlements = findSettlements(baselineStrategy);
        List<Settlement> targetSettlements = findSettlements(targetStrategy);

        assertThat(targetSettlements).hasSameSizeAs(baselineSettlements);
        for (int index = 0; index < baselineSettlements.size(); index++) {
            Settlement baseline = baselineSettlements.get(index);
            Settlement target = targetSettlements.get(index);

            assertThat(target.getMerchant().getId()).isEqualTo(baseline.getMerchant().getId());
            assertThat(target.getTotalPaymentAmount()).isEqualByComparingTo(baseline.getTotalPaymentAmount());
            assertThat(target.getTotalCancelAmount()).isEqualByComparingTo(baseline.getTotalCancelAmount());
            assertThat(target.getNetSalesAmount()).isEqualByComparingTo(baseline.getNetSalesAmount());
            assertThat(target.getFeeAmount()).isEqualByComparingTo(baseline.getFeeAmount());
            assertThat(target.getFinalSettlementAmount()).isEqualByComparingTo(baseline.getFinalSettlementAmount());
        }
    }

    private List<Settlement> findSettlements(SettlementStrategy strategy) {
        return settlementRepository
                .findAllBySettlementDateAndProcessingStrategyOrderByFinalSettlementAmountDesc(
                        SETTLEMENT_DATE,
                        strategy
                )
                .stream()
                .sorted(Comparator.comparing(settlement -> settlement.getMerchant().getId()))
                .toList();
    }

    private void createPaymentFixture() {
        Merchant merchant = merchantRepository.save(new Merchant("merchant-a", new BigDecimal("0.0300")));
        paymentRepository.save(new Payment(
                merchant,
                PaymentType.PAYMENT,
                PaymentStatus.COMPLETED,
                new BigDecimal("10000.00"),
                SETTLEMENT_DATE,
                LocalDateTime.now()
        ));
        paymentRepository.save(new Payment(
                merchant,
                PaymentType.CANCEL,
                PaymentStatus.COMPLETED,
                new BigDecimal("1000.00"),
                SETTLEMENT_DATE,
                LocalDateTime.now()
        ));
    }

    private void createMultiMerchantPaymentFixture() {
        Merchant merchantA = merchantRepository.save(new Merchant("merchant-a", new BigDecimal("0.0300")));
        Merchant merchantB = merchantRepository.save(new Merchant("merchant-b", new BigDecimal("0.0250")));

        paymentRepository.save(new Payment(
                merchantA,
                PaymentType.PAYMENT,
                PaymentStatus.COMPLETED,
                new BigDecimal("10000.00"),
                SETTLEMENT_DATE,
                LocalDateTime.now()
        ));
        paymentRepository.save(new Payment(
                merchantA,
                PaymentType.CANCEL,
                PaymentStatus.COMPLETED,
                new BigDecimal("1000.00"),
                SETTLEMENT_DATE,
                LocalDateTime.now()
        ));
        paymentRepository.save(new Payment(
                merchantB,
                PaymentType.PAYMENT,
                PaymentStatus.COMPLETED,
                new BigDecimal("5000.00"),
                SETTLEMENT_DATE,
                LocalDateTime.now()
        ));
        paymentRepository.save(new Payment(
                merchantB,
                PaymentType.CANCEL,
                PaymentStatus.COMPLETED,
                new BigDecimal("1200.00"),
                SETTLEMENT_DATE,
                LocalDateTime.now()
        ));
        paymentRepository.save(new Payment(
                merchantB,
                PaymentType.PAYMENT,
                PaymentStatus.FAILED,
                new BigDecimal("9999.00"),
                SETTLEMENT_DATE,
                LocalDateTime.now()
        ));
    }

    private Settlement createSettlement(Merchant merchant, LocalDate settlementDate, SettlementStrategy strategy) {
        return new Settlement(
                merchant,
                settlementDate,
                strategy,
                new BigDecimal("10000.00"),
                BigDecimal.ZERO.setScale(2),
                new BigDecimal("10000.00"),
                merchant.getFeeRate(),
                new BigDecimal("300.00"),
                new BigDecimal("9700.00")
        );
    }
}
