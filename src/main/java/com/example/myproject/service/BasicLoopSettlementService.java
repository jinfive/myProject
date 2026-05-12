package com.example.myproject.service;

import com.example.myproject.domain.batch.BatchJobHistory;
import com.example.myproject.domain.batch.SettlementStrategy;
import com.example.myproject.domain.settlement.Settlement;
import com.example.myproject.repository.BatchJobHistoryRepository;
import com.example.myproject.repository.SettlementRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BasicLoopSettlementService {

    private final BatchJobHistoryRepository batchJobHistoryRepository;
    private final SettlementRepository settlementRepository;
    private final BatchJobHistoryService batchJobHistoryService;
    private final BasicLoopSettlementProcessor basicLoopSettlementProcessor;

    public BasicLoopSettlementService(
            BatchJobHistoryRepository batchJobHistoryRepository,
            SettlementRepository settlementRepository,
            BatchJobHistoryService batchJobHistoryService,
            BasicLoopSettlementProcessor basicLoopSettlementProcessor
    ) {
        this.batchJobHistoryRepository = batchJobHistoryRepository;
        this.settlementRepository = settlementRepository;
        this.batchJobHistoryService = batchJobHistoryService;
        this.basicLoopSettlementProcessor = basicLoopSettlementProcessor;
    }

    public SettlementRunResult run(LocalDate settlementDate, SettlementStrategy strategy) {
        if (strategy != SettlementStrategy.BASIC_LOOP) {
            throw new IllegalArgumentException(strategy + " strategy is not implemented yet.");
        }

        LocalDateTime startedAt = LocalDateTime.now();
        BatchJobHistory history = batchJobHistoryService.start(settlementDate, strategy, startedAt);
        try {
            SettlementProcessResult processResult = basicLoopSettlementProcessor.run(settlementDate);
            BatchJobHistory successHistory = batchJobHistoryService.markSuccess(
                    history.getId(),
                    LocalDateTime.now(),
                    processResult.processedCount(),
                    processResult.settlements().size()
            );
            return new SettlementRunResult(successHistory, processResult.settlements());
        } catch (RuntimeException exception) {
            batchJobHistoryService.markFailed(history.getId(), LocalDateTime.now(), exception.getMessage());
            throw exception;
        }
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

    @Transactional(readOnly = true)
    public List<BatchJobHistory> getBatchHistories() {
        return batchJobHistoryRepository.findTop20ByOrderByStartedAtDesc();
    }
}
