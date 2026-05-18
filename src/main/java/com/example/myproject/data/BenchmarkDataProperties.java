package com.example.myproject.data;

import java.time.LocalDate;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "benchmark")
public class BenchmarkDataProperties {

    private String profile = "small";
    private boolean resetEnabled = false;
    private boolean dataDateSyncEnabled = true;
    private Boolean targetDateSyncEnabled;
    private int merchantCount = 100;
    private int paymentCount = 100_000;
    private int batchSize = 1_000;
    private LocalDate targetDate;
    private LocalDate dateDistributionStart;
    private LocalDate dateDistributionEnd;

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public boolean isResetEnabled() {
        return resetEnabled;
    }

    public void setResetEnabled(boolean resetEnabled) {
        this.resetEnabled = resetEnabled;
    }

    public boolean isDataDateSyncEnabled() {
        return targetDateSyncEnabled == null ? dataDateSyncEnabled : targetDateSyncEnabled;
    }

    public void setDataDateSyncEnabled(boolean dataDateSyncEnabled) {
        this.dataDateSyncEnabled = dataDateSyncEnabled;
    }

    public Boolean getTargetDateSyncEnabled() {
        return targetDateSyncEnabled;
    }

    public void setTargetDateSyncEnabled(Boolean targetDateSyncEnabled) {
        this.targetDateSyncEnabled = targetDateSyncEnabled;
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

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    public LocalDate resolveTargetDate() {
        return targetDate == null ? LocalDate.now() : targetDate;
    }

    public LocalDate getDateDistributionStart() {
        return dateDistributionStart;
    }

    public void setDateDistributionStart(LocalDate dateDistributionStart) {
        this.dateDistributionStart = dateDistributionStart;
    }

    public LocalDate getDateDistributionEnd() {
        return dateDistributionEnd;
    }

    public void setDateDistributionEnd(LocalDate dateDistributionEnd) {
        this.dateDistributionEnd = dateDistributionEnd;
    }
}
