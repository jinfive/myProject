package com.example.myproject.data;

import com.example.myproject.domain.merchant.Merchant;
import com.example.myproject.domain.payment.Payment;
import com.example.myproject.domain.payment.PaymentStatus;
import com.example.myproject.domain.payment.PaymentType;
import com.example.myproject.repository.MerchantRepository;
import com.example.myproject.repository.PaymentRepository;
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
    private final DummyDataProperties properties;
    private final EntityManager entityManager;

    public DummyDataService(
            MerchantRepository merchantRepository,
            PaymentRepository paymentRepository,
            DummyDataProperties properties,
            EntityManager entityManager
    ) {
        this.merchantRepository = merchantRepository;
        this.paymentRepository = paymentRepository;
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

        validateProperties();

        List<Long> merchantIds = merchantRepository.saveAll(createMerchants())
                .stream()
                .map(Merchant::getId)
                .toList();
        entityManager.flush();
        entityManager.clear();

        createPayments(merchantIds);

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

    private void validateProperties() {
        if (properties.getMerchantCount() < 100) {
            throw new IllegalArgumentException("dummy-data.merchant-count must be at least 100");
        }
        if (properties.getPaymentCount() < 100_000) {
            throw new IllegalArgumentException("dummy-data.payment-count must be at least 100000");
        }
        if (properties.getTargetDatePaymentCount() <= 0
                || properties.getTargetDatePaymentCount() > properties.getPaymentCount()) {
            throw new IllegalArgumentException("dummy-data.target-date-payment-count must be between 1 and payment-count");
        }
        if (properties.getBatchSize() <= 0) {
            throw new IllegalArgumentException("dummy-data.batch-size must be greater than 0");
        }
    }

    private List<Merchant> createMerchants() {
        List<Merchant> merchants = new ArrayList<>(properties.getMerchantCount());

        for (int i = 1; i <= properties.getMerchantCount(); i++) {
            BigDecimal feeRate = BigDecimal.valueOf(0.015)
                    .add(BigDecimal.valueOf(i).multiply(BigDecimal.valueOf(0.0001)))
                    .setScale(4, RoundingMode.HALF_UP);
            merchants.add(new Merchant("테스트 가맹점 " + i, feeRate));
        }

        return merchants;
    }

    private void createPayments(List<Long> merchantIds) {
        Random random = new Random(RANDOM_SEED);
        List<Payment> batch = new ArrayList<>(properties.getBatchSize());

        for (int i = 0; i < properties.getPaymentCount(); i++) {
            LocalDate transactionDate = resolveTransactionDate(i);
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

            if (batch.size() == properties.getBatchSize()) {
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

    private LocalDate resolveTransactionDate(int index) {
        if (index < properties.getTargetDatePaymentCount()) {
            return properties.getTargetDate();
        }

        int offset = (index - properties.getTargetDatePaymentCount()) % 14;
        return properties.getTargetDate().minusDays(7).plusDays(offset);
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
}
