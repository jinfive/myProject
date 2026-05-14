package com.example.myproject.repository;

import com.example.myproject.domain.merchant.Merchant;
import java.math.BigDecimal;

public record PaymentSettlementAggregation(
        Merchant merchant,
        BigDecimal feeRate,
        BigDecimal totalPaymentAmount,
        BigDecimal totalCancelAmount,
        long processedCount
) {
}
