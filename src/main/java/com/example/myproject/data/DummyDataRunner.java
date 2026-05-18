package com.example.myproject.data;

import com.example.myproject.repository.PaymentRepository;
import java.time.LocalDate;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties({DummyDataProperties.class, BenchmarkDataProperties.class})
public class DummyDataRunner implements ApplicationRunner {

    private final DummyDataService dummyDataService;
    private final DummyDataProperties properties;
    private final BenchmarkDataProperties benchmarkDataProperties;
    private final BenchmarkDataDateSyncService benchmarkDataDateSyncService;
    private final PaymentRepository paymentRepository;

    public DummyDataRunner(
            DummyDataService dummyDataService,
            DummyDataProperties properties,
            BenchmarkDataProperties benchmarkDataProperties,
            BenchmarkDataDateSyncService benchmarkDataDateSyncService,
            PaymentRepository paymentRepository
    ) {
        this.dummyDataService = dummyDataService;
        this.properties = properties;
        this.benchmarkDataProperties = benchmarkDataProperties;
        this.benchmarkDataDateSyncService = benchmarkDataDateSyncService;
        this.paymentRepository = paymentRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }

        if (benchmarkDataProperties.isResetEnabled()) {
            dummyDataService.regenerateBenchmark(benchmarkDataProperties);
            return;
        }

        boolean paymentAlreadyExists = paymentRepository.count() > 0;
        dummyDataService.generateIfEmpty();

        if (paymentAlreadyExists && benchmarkDataProperties.isDataDateSyncEnabled()) {
            benchmarkDataDateSyncService.syncTo(LocalDate.now());
        }
    }
}
