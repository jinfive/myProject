package com.example.myproject.repository;

import java.math.BigDecimal;

public interface PaymentSettlementAggregationProjection {

    Long getMerchantId();

    String getMerchantName();

    BigDecimal getFeeRate();

    BigDecimal getPaymentAmount();

    BigDecimal getCancelAmount();

    long getProcessedCount();
}
