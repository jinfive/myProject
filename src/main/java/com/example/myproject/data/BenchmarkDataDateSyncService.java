package com.example.myproject.data;

import com.example.myproject.repository.PaymentRepository;
import com.example.myproject.repository.SettlementRepository;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BenchmarkDataDateSyncService {

    private static final Logger log = LoggerFactory.getLogger(BenchmarkDataDateSyncService.class);

    private final PaymentRepository paymentRepository;
    private final SettlementRepository settlementRepository;

    public BenchmarkDataDateSyncService(
            PaymentRepository paymentRepository,
            SettlementRepository settlementRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.settlementRepository = settlementRepository;
    }

    @Transactional
    public SyncResult syncTo(LocalDate targetDate) {
        long paymentCount = paymentRepository.count();
        if (paymentCount == 0) {
            log.info("[BenchmarkDataDateSync] payment data does not exist. skip sync.");
            return SyncResult.skippedNoPayments(targetDate);
        }

        log.info("[BenchmarkDataDateSync] payment data already exists. count={}", paymentCount);

        boolean syncRequired = paymentRepository.existsByTransactionDateNot(targetDate);
        LocalDate minDate = paymentRepository.findMinTransactionDate();
        LocalDate maxDate = paymentRepository.findMaxTransactionDate();

        if (!syncRequired) {
            log.info("[BenchmarkDataDateSync] payment_date already matches today. skip sync. targetDate={}", targetDate);
            return SyncResult.skippedAlreadySynced(paymentCount, targetDate, minDate, maxDate);
        }

        log.info(
                "[BenchmarkDataDateSync] payment_date is not today. sync required. targetDate={}, currentDateRange={}~{}",
                targetDate,
                minDate,
                maxDate
        );

        long settlementCount = settlementRepository.count();
        int deletedSettlementCount = settlementRepository.deleteAllInBulk();
        log.info("[BenchmarkDataDateSync] deleted settlements. count={}", deletedSettlementCount);

        int updatedPaymentCount = paymentRepository.updateTransactionDate(targetDate);
        log.info("[BenchmarkDataDateSync] updated payment_date. count={}, targetDate={}", updatedPaymentCount, targetDate);
        log.info("[BenchmarkDataDateSync] batch_job_histories are preserved.");

        return SyncResult.synced(
                paymentCount,
                targetDate,
                minDate,
                maxDate,
                updatedPaymentCount,
                Math.max(settlementCount, deletedSettlementCount)
        );
    }

    public record SyncResult(
            boolean syncExecuted,
            boolean paymentExists,
            long paymentCount,
            LocalDate targetDate,
            LocalDate beforeMinDate,
            LocalDate beforeMaxDate,
            long updatedPaymentCount,
            long deletedSettlementCount
    ) {

        private static SyncResult skippedNoPayments(LocalDate targetDate) {
            return new SyncResult(false, false, 0, targetDate, null, null, 0, 0);
        }

        private static SyncResult skippedAlreadySynced(
                long paymentCount,
                LocalDate targetDate,
                LocalDate beforeMinDate,
                LocalDate beforeMaxDate
        ) {
            return new SyncResult(false, true, paymentCount, targetDate, beforeMinDate, beforeMaxDate, 0, 0);
        }

        private static SyncResult synced(
                long paymentCount,
                LocalDate targetDate,
                LocalDate beforeMinDate,
                LocalDate beforeMaxDate,
                long updatedPaymentCount,
                long deletedSettlementCount
        ) {
            return new SyncResult(
                    true,
                    true,
                    paymentCount,
                    targetDate,
                    beforeMinDate,
                    beforeMaxDate,
                    updatedPaymentCount,
                    deletedSettlementCount
            );
        }
    }
}
