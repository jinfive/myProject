package com.example.myproject.api;

import com.example.myproject.domain.settlement.Settlement;
import java.math.BigDecimal;
import java.util.List;

public record SettlementSummaryResponse(
        long processedCount,
        BigDecimal totalPaymentAmount,
        BigDecimal totalCancelAmount,
        BigDecimal totalFeeAmount,
        BigDecimal totalSettlementAmount,
        long elapsedMs,
        List<SettlementResponse> settlements
) {

    public static SettlementSummaryResponse of(
            long processedCount,
            long elapsedMs,
            List<Settlement> settlements
    ) {
        BigDecimal totalPaymentAmount = BigDecimal.ZERO;
        BigDecimal totalCancelAmount = BigDecimal.ZERO;
        BigDecimal totalFeeAmount = BigDecimal.ZERO;
        BigDecimal totalSettlementAmount = BigDecimal.ZERO;

        for (Settlement settlement : settlements) {
            totalPaymentAmount = totalPaymentAmount.add(settlement.getTotalPaymentAmount());
            totalCancelAmount = totalCancelAmount.add(settlement.getTotalCancelAmount());
            totalFeeAmount = totalFeeAmount.add(settlement.getFeeAmount());
            totalSettlementAmount = totalSettlementAmount.add(settlement.getFinalSettlementAmount());
        }

        return new SettlementSummaryResponse(
                processedCount,
                totalPaymentAmount,
                totalCancelAmount,
                totalFeeAmount,
                totalSettlementAmount,
                elapsedMs,
                settlements.stream().map(SettlementResponse::from).toList()
        );
    }
}
