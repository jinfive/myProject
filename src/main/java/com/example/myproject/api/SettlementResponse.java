package com.example.myproject.api;

import com.example.myproject.domain.batch.SettlementStrategy;
import com.example.myproject.domain.settlement.Settlement;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SettlementResponse(
        Long id,
        LocalDate settlementDate,
        SettlementStrategy strategy,
        Long merchantId,
        String merchantName,
        BigDecimal totalPaymentAmount,
        BigDecimal totalCancelAmount,
        BigDecimal netSalesAmount,
        BigDecimal feeRate,
        BigDecimal feeAmount,
        BigDecimal finalSettlementAmount
) {

    public static SettlementResponse from(Settlement settlement) {
        return new SettlementResponse(
                settlement.getId(),
                settlement.getSettlementDate(),
                settlement.getProcessingStrategy(),
                settlement.getMerchant().getId(),
                settlement.getMerchant().getName(),
                settlement.getTotalPaymentAmount(),
                settlement.getTotalCancelAmount(),
                settlement.getNetSalesAmount(),
                settlement.getFeeRate(),
                settlement.getFeeAmount(),
                settlement.getFinalSettlementAmount()
        );
    }
}
