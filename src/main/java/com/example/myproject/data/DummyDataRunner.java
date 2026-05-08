package com.example.myproject.data;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(DummyDataProperties.class)
public class DummyDataRunner implements ApplicationRunner {

    private final DummyDataService dummyDataService;
    private final DummyDataProperties properties;

    public DummyDataRunner(DummyDataService dummyDataService, DummyDataProperties properties) {
        this.dummyDataService = dummyDataService;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }

        dummyDataService.generateIfEmpty();
    }
}
