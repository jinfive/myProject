package com.example.myproject.domain.payment;

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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate transactionDate;

    @Column(nullable = false)
    private LocalDateTime approvedAt;

    protected Payment() {
    }

    public Payment(
            Merchant merchant,
            PaymentType type,
            PaymentStatus status,
            BigDecimal amount,
            LocalDate transactionDate,
            LocalDateTime approvedAt
    ) {
        this.merchant = merchant;
        this.type = type;
        this.status = status;
        this.amount = amount;
        this.transactionDate = transactionDate;
        this.approvedAt = approvedAt;
    }

    public Long getId() {
        return id;
    }

    public Merchant getMerchant() {
        return merchant;
    }

    public PaymentType getType() {
        return type;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }
}
