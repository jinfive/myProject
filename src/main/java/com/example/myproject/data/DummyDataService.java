package com.example.myproject.data;

import com.example.myproject.domain.merchant.Merchant;
import com.example.myproject.domain.payment.Payment;
import com.example.myproject.domain.payment.PaymentStatus;
import com.example.myproject.domain.payment.PaymentType;
import com.example.myproject.repository.MerchantRepository;
import com.example.myproject.repository.PaymentRepository;
import com.example.myproject.repository.SettlementRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DummyDataService {

    private static final Logger log = LoggerFactory.getLogger(DummyDataService.class);
    private static final int RANDOM_SEED = 20260508;

    private final MerchantRepository merchantRepository;
    private final PaymentRepository paymentRepository;
    private final SettlementRepository settlementRepository;
    private final DummyDataProperties properties;
    private final EntityManager entityManager;

    public DummyDataService(
            MerchantRepository merchantRepository,
            PaymentRepository paymentRepository,
            SettlementRepository settlementRepository,
            DummyDataProperties properties,
            EntityManager entityManager
    ) {
        this.merchantRepository = merchantRepository;
        this.paymentRepository = paymentRepository;
        this.settlementRepository = settlementRepository;
        this.properties = properties;
        this.entityManager = entityManager;
    }

    @Transactional
    public void generateIfEmpty() {
        LocalDateTime startedAt = LocalDateTime.now();
        log.info("Dummy data generation started at {}", startedAt);

        long merchantCount = merchantRepository.count();
        long paymentCount = paymentRepository.count();
        long targetDatePaymentCount = paymentRepository.countByTransactionDate(properties.getTargetDate());

        if (merchantCount > 0 || paymentCount > 0) {
            log.info(
                    "Dummy data already exists. Skip generation. merchantCount={}, paymentCount={}, targetDate={}, targetDatePaymentCount={}",
                    merchantCount,
                    paymentCount,
                    properties.getTargetDate(),
                    targetDatePaymentCount
            );
            return;
        }

        GenerationSpec spec = GenerationSpec.from(properties);
        validateSpec(spec, true);

        generate(spec);

        LocalDateTime endedAt = LocalDateTime.now();
        long elapsedMs = Duration.between(startedAt, endedAt).toMillis();

        log.info("Dummy data generation finished at {}", endedAt);
        log.info("Dummy data generation elapsedMs={}", elapsedMs);
        log.info("Merchant total count={}", merchantRepository.count());
        log.info("Payment total count={}", paymentRepository.count());
        log.info(
                "Payment count for {}={}",
                properties.getTargetDate(),
                paymentRepository.countByTransactionDate(properties.getTargetDate())
        );
    }

    @Transactional
    public BenchmarkGenerationResult regenerateBenchmark(BenchmarkDataProperties benchmarkProperties) {
        GenerationSpec spec = GenerationSpec.from(benchmarkProperties);
        validateSpec(spec, false);

        LocalDateTime startedAt = LocalDateTime.now();
        log.info(
                "[BenchmarkDataReset] benchmark regeneration started. profile={}, merchantCount={}, paymentCount={}, targetDate={}",
                benchmarkProperties.getProfile(),
                spec.merchantCount(),
                spec.paymentCount(),
                spec.targetDate()
        );

        int deletedSettlementCount = settlementRepository.deleteAllInBulk();
        int deletedPaymentCount = paymentRepository.deleteAllInBulk();
        int deletedMerchantCount = merchantRepository.deleteAllInBulk();
        entityManager.flush();
        entityManager.clear();

        log.info(
                "[BenchmarkDataReset] deleted benchmark data. settlements={}, payments={}, merchants={}",
                deletedSettlementCount,
                deletedPaymentCount,
                deletedMerchantCount
        );
        log.info("[BenchmarkDataReset] batch_job_histories are preserved.");

        generate(spec);

        LocalDateTime endedAt = LocalDateTime.now();
        long elapsedMs = Duration.between(startedAt, endedAt).toMillis();

        long merchantCount = merchantRepository.count();
        long paymentCount = paymentRepository.count();
        long targetDatePaymentCount = paymentRepository.countByTransactionDate(spec.targetDate());

        log.info("[BenchmarkDataReset] benchmark regeneration finished. elapsedMs={}", elapsedMs);
        log.info(
                "[BenchmarkDataReset] generated counts. merchants={}, payments={}, targetDate={}, targetDatePayments={}",
                merchantCount,
                paymentCount,
                spec.targetDate(),
                targetDatePaymentCount
        );

        return new BenchmarkGenerationResult(
                benchmarkProperties.getProfile(),
                spec.targetDate(),
                merchantCount,
                paymentCount,
                targetDatePaymentCount,
                deletedSettlementCount,
                deletedPaymentCount,
                deletedMerchantCount,
                elapsedMs
        );
    }

    private void validateSpec(GenerationSpec spec, boolean strictMinimums) {
        if (strictMinimums && spec.merchantCount() < 100) {
            throw new IllegalArgumentException("dummy-data.merchant-count must be at least 100");
        }
        if (!strictMinimums && spec.merchantCount() <= 0) {
            throw new IllegalArgumentException("benchmark.merchant-count must be greater than 0");
        }
        if (strictMinimums && spec.paymentCount() < 100_000) {
            throw new IllegalArgumentException("dummy-data.payment-count must be at least 100000");
        }
        if (!strictMinimums && spec.paymentCount() <= 0) {
            throw new IllegalArgumentException("benchmark.payment-count must be greater than 0");
        }
        if (spec.targetDatePaymentCount() <= 0 || spec.targetDatePaymentCount() > spec.paymentCount()) {
            throw new IllegalArgumentException("target-date-payment-count must be between 1 and payment-count");
        }
        if (spec.batchSize() <= 0) {
            throw new IllegalArgumentException("dummy-data.batch-size must be greater than 0");
        }
    }

    private void generate(GenerationSpec spec) {
        List<Long> merchantIds = merchantRepository.saveAll(createMerchants(spec))
                .stream()
                .map(Merchant::getId)
                .toList();
        entityManager.flush();
        entityManager.clear();

        createPayments(merchantIds, spec);
    }

    private List<Merchant> createMerchants(GenerationSpec spec) {
        List<Merchant> merchants = new ArrayList<>(spec.merchantCount());

        for (int i = 1; i <= spec.merchantCount(); i++) {
            BigDecimal feeRate = BigDecimal.valueOf(0.0150)
                    .add(BigDecimal.valueOf((i - 1) % 20).multiply(BigDecimal.valueOf(0.0005)))
                    .setScale(4, RoundingMode.HALF_UP);
            merchants.add(new Merchant("테스트 가맹점 " + i, feeRate));
        }

        return merchants;
    }

    private void createPayments(List<Long> merchantIds, GenerationSpec spec) {
        Random random = new Random(RANDOM_SEED);
        List<Payment> batch = new ArrayList<>(spec.batchSize());

        for (int i = 0; i < spec.paymentCount(); i++) {
            LocalDate transactionDate = resolveTransactionDate(i, spec);
            Merchant merchant = entityManager.getReference(Merchant.class, merchantIds.get(i % merchantIds.size()));
            PaymentType type = resolvePaymentType(i);
            BigDecimal amount = createAmount(random, type);
            LocalDateTime approvedAt = transactionDate.atTime(i % 24, i % 60, i % 60);

            batch.add(new Payment(
                    merchant,
                    type,
                    PaymentStatus.COMPLETED,
                    amount,
                    transactionDate,
                    approvedAt
            ));

            if (batch.size() == spec.batchSize()) {
                paymentRepository.saveAll(batch);
                entityManager.flush();
                entityManager.clear();
                batch.clear();
            }
        }

        if (!batch.isEmpty()) {
            paymentRepository.saveAll(batch);
            entityManager.flush();
            entityManager.clear();
        }
    }

    private LocalDate resolveTransactionDate(int index, GenerationSpec spec) {
        if (index < spec.targetDatePaymentCount()) {
            return spec.targetDate();
        }

        int offset = (index - spec.targetDatePaymentCount()) % 14;
        if (offset >= 7) {
            offset++;
        }
        return spec.targetDate().minusDays(7).plusDays(offset);
    }

    private PaymentType resolvePaymentType(int index) {
        return index % 10 < 8 ? PaymentType.PAYMENT : PaymentType.CANCEL;
    }

    private BigDecimal createAmount(Random random, PaymentType type) {
        int baseAmount = type == PaymentType.PAYMENT
                ? 10_000 + random.nextInt(190_001)
                : 5_000 + random.nextInt(95_001);
        return BigDecimal.valueOf(baseAmount).setScale(2, RoundingMode.HALF_UP);
    }

    private record GenerationSpec(
            int merchantCount,
            int paymentCount,
            int targetDatePaymentCount,
            LocalDate targetDate,
            int batchSize
    ) {

        private static GenerationSpec from(DummyDataProperties properties) {
            return new GenerationSpec(
                    properties.getMerchantCount(),
                    properties.getPaymentCount(),
                    properties.getTargetDatePaymentCount(),
                    properties.getTargetDate(),
                    properties.getBatchSize()
            );
        }

        private static GenerationSpec from(BenchmarkDataProperties properties) {
            return new GenerationSpec(
                    properties.getMerchantCount(),
                    properties.getPaymentCount(),
                    properties.getPaymentCount(),
                    properties.resolveTargetDate(),
                    properties.getBatchSize()
            );
        }
    }

    public record BenchmarkGenerationResult(
            String profile,
            LocalDate targetDate,
            long merchantCount,
            long paymentCount,
            long targetDatePaymentCount,
            long deletedSettlementCount,
            long deletedPaymentCount,
            long deletedMerchantCount,
            long elapsedMs
    ) {
    }
}
