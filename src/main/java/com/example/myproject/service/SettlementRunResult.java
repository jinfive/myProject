package com.example.myproject.service;

import com.example.myproject.domain.batch.BatchJobHistory;
import com.example.myproject.domain.settlement.Settlement;
import java.util.List;

public record SettlementRunResult(
        BatchJobHistory batchJobHistory,
        List<Settlement> settlements
) {
}
