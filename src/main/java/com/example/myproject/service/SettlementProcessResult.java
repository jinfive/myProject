package com.example.myproject.service;

import com.example.myproject.domain.settlement.Settlement;
import java.util.List;

public record SettlementProcessResult(
        long processedCount,
        List<Settlement> settlements
) {
}
