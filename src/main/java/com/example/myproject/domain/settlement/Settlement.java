package com.example.myproject.domain.settlement;

import com.example.myproject.domain.batch.SettlementStrategy;
import com.example.myproject.domain.merchant.Merchant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "settlements",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_settlements_merchant_date_strategy",
                        columnNames = {"merchant_id", "settlement_date", "processing_strategy"}
                )
        }
)
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Column(nullable = false)
    private LocalDate settlementDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_strategy", nullable = false, length = 30, columnDefinition = "varchar(30) default 'BASIC_LOOP'")
    private SettlementStrategy processingStrategy;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalPaymentAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalCancelAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal netSalesAmount;

    @Column(nullable = false, precision = 6, scale = 4)
    private BigDecimal feeRate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal feeAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal finalSettlementAmount;

    protected Settlement() {
    }

    public Settlement(
            Merchant merchant,
            LocalDate settlementDate,
            SettlementStrategy processingStrategy,
            BigDecimal totalPaymentAmount,
            BigDecimal totalCancelAmount,
            BigDecimal netSalesAmount,
            BigDecimal feeRate,
            BigDecimal feeAmount,
            BigDecimal finalSettlementAmount
    ) {
        this.merchant = merchant;
        this.settlementDate = settlementDate;
        this.processingStrategy = processingStrategy;
        this.totalPaymentAmount = totalPaymentAmount;
        this.totalCancelAmount = totalCancelAmount;
        this.netSalesAmount = netSalesAmount;
        this.feeRate = feeRate;
        this.feeAmount = feeAmount;
        this.finalSettlementAmount = finalSettlementAmount;
    }

    public Long getId() {
        return id;
    }

    public Merchant getMerchant() {
        return merchant;
    }

    public LocalDate getSettlementDate() {
        return settlementDate;
    }

    public SettlementStrategy getProcessingStrategy() {
        return processingStrategy;
    }

    public BigDecimal getTotalPaymentAmount() {
        return totalPaymentAmount;
    }

    public BigDecimal getTotalCancelAmount() {
        return totalCancelAmount;
    }

    public BigDecimal getNetSalesAmount() {
        return netSalesAmount;
    }

    public BigDecimal getFeeRate() {
        return feeRate;
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    public BigDecimal getFinalSettlementAmount() {
        return finalSettlementAmount;
    }
}
