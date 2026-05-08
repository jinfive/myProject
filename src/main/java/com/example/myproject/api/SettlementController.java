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
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SettlementController {

    private final BasicLoopSettlementService settlementService;

    public SettlementController(BasicLoopSettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @PostMapping("/settlements/run")
    public SettlementSummaryResponse runSettlement(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "BASIC_LOOP") SettlementStrategy strategy
    ) {
        SettlementRunResult result = settlementService.run(date, strategy);
        return SettlementSummaryResponse.of(
                result.batchJobHistory().getProcessedCount(),
                result.batchJobHistory().getElapsedMs(),
                result.settlements()
        );
    }

    @GetMapping("/settlements")
    public SettlementSummaryResponse getSettlements(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<Settlement> settlements = settlementService.getSettlements(date);
        BatchJobHistory latestHistory = settlementService.getBatchHistories()
                .stream()
                .filter(history -> history.getSettlementDate().equals(date))
                .findFirst()
                .orElse(null);

        long processedCount = latestHistory == null ? 0 : latestHistory.getProcessedCount();
        long elapsedMs = latestHistory == null ? 0 : latestHistory.getElapsedMs();
        return SettlementSummaryResponse.of(processedCount, elapsedMs, settlements);
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
}
