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
import com.example.myproject.repository.SettlementRepository;
import com.example.myproject.service.BasicLoopSettlementService;
import com.example.myproject.service.GroupByBulkSaveSettlementProcessor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@SpringBootTest
class GroupByBulkSaveSettlementFailureHistoryIntegrationTests {

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
    void failedGroupByBulkSaveRollsBackResultsButKeepsFailedHistory() {
        Merchant merchant = merchantRepository.save(new Merchant("merchant-a", new BigDecimal("0.0300")));
        paymentRepository.save(new Payment(
                merchant,
                PaymentType.PAYMENT,
                PaymentStatus.COMPLETED,
                new BigDecimal("10000.00"),
                SETTLEMENT_DATE,
                LocalDateTime.now()
        ));

        assertThatThrownBy(() -> settlementService.run(SETTLEMENT_DATE, SettlementStrategy.GROUP_BY_BULK_SAVE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("forced group by bulk save settlement failure");

        assertThat(settlementRepository.count()).isZero();
        BatchJobHistory history = batchJobHistoryRepository.findAll().get(0);
        assertThat(history.getStatus()).isEqualTo(BatchJobStatus.FAILED);
        assertThat(history.getStrategy()).isEqualTo(SettlementStrategy.GROUP_BY_BULK_SAVE);
        assertThat(history.getErrorMessage()).contains("forced group by bulk save settlement failure");
        assertThat(history.getEndedAt()).isNotNull();
    }

    @TestConfiguration
    static class FailureProcessorConfig {

        @Bean
        @Primary
        GroupByBulkSaveSettlementProcessor failingGroupByBulkSaveProcessor(
                PaymentRepository paymentRepository,
                SettlementRepository settlementRepository
        ) {
            return new GroupByBulkSaveSettlementProcessor(paymentRepository, settlementRepository) {
                @Override
                protected void afterSettlementsSaved(List<Settlement> settlements) {
                    throw new IllegalStateException("forced group by bulk save settlement failure");
                }
            };
        }
    }
}
