package com.example.myproject.service;

import com.example.myproject.domain.batch.BatchJobHistory;
import com.example.myproject.domain.batch.SettlementStrategy;
import com.example.myproject.repository.BatchJobHistoryRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BatchJobHistoryService {

    private final BatchJobHistoryRepository batchJobHistoryRepository;

    public BatchJobHistoryService(BatchJobHistoryRepository batchJobHistoryRepository) {
        this.batchJobHistoryRepository = batchJobHistoryRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BatchJobHistory start(LocalDate settlementDate, SettlementStrategy strategy, LocalDateTime startedAt) {
        return batchJobHistoryRepository.save(BatchJobHistory.running(settlementDate, strategy, startedAt));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BatchJobHistory markSuccess(Long historyId, LocalDateTime endedAt, long processedCount, long successCount) {
        BatchJobHistory history = batchJobHistoryRepository.findById(historyId)
                .orElseThrow(() -> new IllegalStateException("Batch history not found: " + historyId));
        history.markSuccess(endedAt, processedCount, successCount);
        return history;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BatchJobHistory markFailed(Long historyId, LocalDateTime endedAt, String errorMessage) {
        BatchJobHistory history = batchJobHistoryRepository.findById(historyId)
                .orElseThrow(() -> new IllegalStateException("Batch history not found: " + historyId));
        history.markFailed(endedAt, abbreviate(errorMessage));
        return history;
    }

    private String abbreviate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
