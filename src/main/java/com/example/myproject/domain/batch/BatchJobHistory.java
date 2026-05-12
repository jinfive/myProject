package com.example.myproject.domain.batch;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "batch_job_histories")
public class BatchJobHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate settlementDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_strategy", nullable = false, length = 30, columnDefinition = "varchar(30) default 'BASIC_LOOP'")
    private SettlementStrategy strategy;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    private long elapsedMs;

    @Column(nullable = false)
    private long processedCount;

    @Column(nullable = false)
    private long successCount;

    @Column(nullable = false)
    private long failureCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BatchJobStatus status;

    @Column(length = 1000)
    private String errorMessage;

    protected BatchJobHistory() {
    }

    public BatchJobHistory(
            LocalDate settlementDate,
            SettlementStrategy strategy,
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            long elapsedMs,
            long processedCount,
            long successCount,
            long failureCount,
            BatchJobStatus status,
            String errorMessage
    ) {
        this.settlementDate = settlementDate;
        this.strategy = strategy;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.elapsedMs = elapsedMs;
        this.processedCount = processedCount;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.status = status;
        this.errorMessage = errorMessage;
    }

    public static BatchJobHistory running(LocalDate settlementDate, SettlementStrategy strategy, LocalDateTime startedAt) {
        return new BatchJobHistory(
                settlementDate,
                strategy,
                startedAt,
                null,
                0,
                0,
                0,
                0,
                BatchJobStatus.RUNNING,
                null
        );
    }

    public void markSuccess(LocalDateTime endedAt, long processedCount, long successCount) {
        this.endedAt = endedAt;
        this.elapsedMs = java.time.Duration.between(startedAt, endedAt).toMillis();
        this.processedCount = processedCount;
        this.successCount = successCount;
        this.failureCount = 0;
        this.status = BatchJobStatus.SUCCESS;
        this.errorMessage = null;
    }

    public void markFailed(LocalDateTime endedAt, String errorMessage) {
        this.endedAt = endedAt;
        this.elapsedMs = java.time.Duration.between(startedAt, endedAt).toMillis();
        this.failureCount = 1;
        this.status = BatchJobStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getSettlementDate() {
        return settlementDate;
    }

    public SettlementStrategy getStrategy() {
        return strategy;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }

    public long getProcessedCount() {
        return processedCount;
    }

    public long getSuccessCount() {
        return successCount;
    }

    public long getFailureCount() {
        return failureCount;
    }

    public BatchJobStatus getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
