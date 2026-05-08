package com.example.myproject.api;

import com.example.myproject.domain.batch.BatchJobHistory;
import com.example.myproject.domain.batch.BatchJobStatus;
import com.example.myproject.domain.batch.SettlementStrategy;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record BatchJobHistoryResponse(
        Long id,
        LocalDate settlementDate,
        SettlementStrategy strategy,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        long elapsedMs,
        long processedCount,
        long successCount,
        long failureCount,
        BatchJobStatus status
) {

    public static BatchJobHistoryResponse from(BatchJobHistory history) {
        return new BatchJobHistoryResponse(
                history.getId(),
                history.getSettlementDate(),
                history.getStrategy(),
                history.getStartedAt(),
                history.getEndedAt(),
                history.getElapsedMs(),
                history.getProcessedCount(),
                history.getSuccessCount(),
                history.getFailureCount(),
                history.getStatus()
        );
    }
}
