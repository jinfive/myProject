package com.example.myproject.data;

import java.time.LocalDate;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dummy-data")
public class DummyDataProperties {

    private boolean enabled = true;
    private int merchantCount = 100;
    private int paymentCount = 100_000;
    private int targetDatePaymentCount = 70_000;
    private LocalDate targetDate = LocalDate.of(2026, 5, 8);
    private int batchSize = 1_000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMerchantCount() {
        return merchantCount;
    }

    public void setMerchantCount(int merchantCount) {
        this.merchantCount = merchantCount;
    }

    public int getPaymentCount() {
        return paymentCount;
    }

    public void setPaymentCount(int paymentCount) {
        this.paymentCount = paymentCount;
    }

    public int getTargetDatePaymentCount() {
        return targetDatePaymentCount;
    }

    public void setTargetDatePaymentCount(int targetDatePaymentCount) {
        this.targetDatePaymentCount = targetDatePaymentCount;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
}
