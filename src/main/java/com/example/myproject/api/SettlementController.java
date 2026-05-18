package com.example.myproject.api;

import com.example.myproject.domain.batch.BatchJobHistory;
import com.example.myproject.domain.batch.SettlementStrategy;
import com.example.myproject.domain.settlement.Settlement;
import com.example.myproject.service.BasicLoopSettlementService;
import com.example.myproject.service.SettlementRunResult;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SettlementController {

    private static final Logger log = LoggerFactory.getLogger(SettlementController.class);

    private final BasicLoopSettlementService settlementService;

    public SettlementController(BasicLoopSettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @PostMapping("/settlements/run")
    public SettlementSummaryResponse runSettlement(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "BASIC_LOOP") SettlementStrategy strategy
    ) {
        long apiStartedAt = System.nanoTime();
        SettlementRunResult result = settlementService.run(date, strategy);
        long responseStartedAt = System.nanoTime();
        SettlementSummaryResponse response = SettlementSummaryResponse.of(
                strategy,
                result.batchJobHistory().getProcessedCount(),
                result.batchJobHistory().getElapsedMs(),
                result.settlements()
        );
        long responseElapsedNanos = System.nanoTime() - responseStartedAt;
        long apiElapsedNanos = System.nanoTime() - apiStartedAt;
        log.info(
                "settlement timing date={} strategy={} section=api response_ms={} total_ms={} processed_count={} settlement_count={}",
                date,
                strategy,
                toMillis(responseElapsedNanos),
                toMillis(apiElapsedNanos),
                response.processedCount(),
                response.settlements().size()
        );
        return response;
    }

    @GetMapping("/settlements")
    public SettlementSummaryResponse getSettlements(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) SettlementStrategy strategy
    ) {
        List<Settlement> settlements = settlementService.getSettlements(date, strategy);
        BatchJobHistory latestHistory = settlementService.getBatchHistories()
                .stream()
                .filter(history -> history.getSettlementDate().equals(date))
                .filter(history -> strategy == null || history.getStrategy() == strategy)
                .findFirst()
                .orElse(null);

        long processedCount = latestHistory == null ? 0 : latestHistory.getProcessedCount();
        long elapsedMs = latestHistory == null ? 0 : latestHistory.getElapsedMs();
        return SettlementSummaryResponse.of(strategy, processedCount, elapsedMs, settlements);
    }

    @DeleteMapping("/settlements")
    public ResetSettlementsResponse resetSettlements(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        int deletedCount = settlementService.resetSettlements(date);
        return new ResetSettlementsResponse(date, deletedCount);
    }

    @GetMapping("/batch-histories")
    public List<BatchJobHistoryResponse> getBatchHistories() {
        return settlementService.getBatchHistories()
                .stream()
                .map(BatchJobHistoryResponse::from)
                .toList();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleConflict(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(exception.getMessage()));
    }

    public record ErrorResponse(String message) {
    }

    public record ResetSettlementsResponse(LocalDate date, int deletedCount) {
    }

    private double toMillis(long nanos) {
        return nanos / 1_000_000.0;
    }
}
