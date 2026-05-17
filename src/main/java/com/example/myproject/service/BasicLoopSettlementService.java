package com.example.myproject.service;

import com.example.myproject.domain.batch.BatchJobHistory;
import com.example.myproject.domain.batch.SettlementStrategy;
import com.example.myproject.domain.settlement.Settlement;
import com.example.myproject.repository.BatchJobHistoryRepository;
import com.example.myproject.repository.SettlementRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BasicLoopSettlementService {

    private static final Logger log = LoggerFactory.getLogger(BasicLoopSettlementService.class);

    private final BatchJobHistoryRepository batchJobHistoryRepository;
    private final SettlementRepository settlementRepository;
    private final BatchJobHistoryService batchJobHistoryService;
    private final BasicLoopSettlementProcessor basicLoopSettlementProcessor;
    private final GroupBySettlementProcessor groupBySettlementProcessor;
    private final GroupByBulkSaveSettlementProcessor groupByBulkSaveSettlementProcessor;

    public BasicLoopSettlementService(
            BatchJobHistoryRepository batchJobHistoryRepository,
            SettlementRepository settlementRepository,
            BatchJobHistoryService batchJobHistoryService,
            BasicLoopSettlementProcessor basicLoopSettlementProcessor,
            GroupBySettlementProcessor groupBySettlementProcessor,
            GroupByBulkSaveSettlementProcessor groupByBulkSaveSettlementProcessor
    ) {
        this.batchJobHistoryRepository = batchJobHistoryRepository;
        this.settlementRepository = settlementRepository;
        this.batchJobHistoryService = batchJobHistoryService;
        this.basicLoopSettlementProcessor = basicLoopSettlementProcessor;
        this.groupBySettlementProcessor = groupBySettlementProcessor;
        this.groupByBulkSaveSettlementProcessor = groupByBulkSaveSettlementProcessor;
    }

    public SettlementRunResult run(LocalDate settlementDate, SettlementStrategy strategy) {
        if (strategy == SettlementStrategy.GROUP_BY_BULK_INDEX) {
            throw new IllegalArgumentException(strategy + " strategy is not implemented yet.");
        }

        LocalDateTime startedAt = LocalDateTime.now();
        long historyStartStartedAt = System.nanoTime();
        BatchJobHistory history = batchJobHistoryService.start(settlementDate, strategy, startedAt);
        long historyStartElapsedNanos = System.nanoTime() - historyStartStartedAt;
        log.info(
                "settlement timing date={} strategy={} section=history_start ms={}",
                settlementDate,
                strategy,
                toMillis(historyStartElapsedNanos)
        );
        try {
            SettlementProcessResult processResult = runProcessor(settlementDate, strategy);
            long historyCompleteStartedAt = System.nanoTime();
            BatchJobHistory successHistory = batchJobHistoryService.markSuccess(
                    history.getId(),
                    LocalDateTime.now(),
                    processResult.processedCount(),
                    processResult.settlements().size()
            );
            long historyCompleteElapsedNanos = System.nanoTime() - historyCompleteStartedAt;
            log.info(
                    "settlement timing date={} strategy={} section=history_complete status=SUCCESS ms={}",
                    settlementDate,
                    strategy,
                    toMillis(historyCompleteElapsedNanos)
            );
            return new SettlementRunResult(successHistory, processResult.settlements());
        } catch (RuntimeException exception) {
            long historyCompleteStartedAt = System.nanoTime();
            batchJobHistoryService.markFailed(history.getId(), LocalDateTime.now(), exception.getMessage());
            long historyCompleteElapsedNanos = System.nanoTime() - historyCompleteStartedAt;
            log.info(
                    "settlement timing date={} strategy={} section=history_complete status=FAILED ms={}",
                    settlementDate,
                    strategy,
                    toMillis(historyCompleteElapsedNanos)
            );
            throw exception;
        }
    }

    private SettlementProcessResult runProcessor(LocalDate settlementDate, SettlementStrategy strategy) {
        if (strategy == SettlementStrategy.BASIC_LOOP) {
            return basicLoopSettlementProcessor.run(settlementDate);
        }
        if (strategy == SettlementStrategy.GROUP_BY_QUERY) {
            return groupBySettlementProcessor.run(settlementDate);
        }
        if (strategy == SettlementStrategy.GROUP_BY_BULK_SAVE) {
            return groupByBulkSaveSettlementProcessor.run(settlementDate);
        }
        throw new IllegalArgumentException(strategy + " strategy is not implemented yet.");
    }

    @Transactional(readOnly = true)
    public List<Settlement> getSettlements(LocalDate settlementDate, SettlementStrategy strategy) {
        if (strategy == null) {
            return settlementRepository.findAllBySettlementDateOrderByFinalSettlementAmountDesc(settlementDate);
        }
        return settlementRepository.findAllBySettlementDateAndProcessingStrategyOrderByFinalSettlementAmountDesc(
                settlementDate,
                strategy
        );
    }

    @Transactional
    public int resetSettlements(LocalDate settlementDate) {
        return settlementRepository.deleteAllBySettlementDate(settlementDate);
    }

    @Transactional(readOnly = true)
    public List<BatchJobHistory> getBatchHistories() {
        return batchJobHistoryRepository.findTop20ByOrderByStartedAtDesc();
    }

    private double toMillis(long nanos) {
        return nanos / 1_000_000.0;
    }
}
