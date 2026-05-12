package com.example.myproject.data;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "benchmark")
public class BenchmarkDataProperties {

    private boolean dataDateSyncEnabled = true;

    public boolean isDataDateSyncEnabled() {
        return dataDateSyncEnabled;
    }

    public void setDataDateSyncEnabled(boolean dataDateSyncEnabled) {
        this.dataDateSyncEnabled = dataDateSyncEnabled;
    }
}
