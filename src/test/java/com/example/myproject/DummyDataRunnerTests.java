package com.example.myproject;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.myproject.data.BenchmarkDataDateSyncService;
import com.example.myproject.data.BenchmarkDataProperties;
import com.example.myproject.data.DummyDataProperties;
import com.example.myproject.data.DummyDataRunner;
import com.example.myproject.data.DummyDataService;
import com.example.myproject.repository.PaymentRepository;
import org.junit.jupiter.api.Test;

class DummyDataRunnerTests {

    @Test
    void generatesDummyDataButDoesNotSyncDateWhenPaymentsDoNotExistBeforeStartup() {
        DummyDataService dummyDataService = mock(DummyDataService.class);
        BenchmarkDataDateSyncService syncService = mock(BenchmarkDataDateSyncService.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        DummyDataProperties dummyDataProperties = new DummyDataProperties();
        BenchmarkDataProperties benchmarkDataProperties = new BenchmarkDataProperties();

        when(paymentRepository.count()).thenReturn(0L);

        DummyDataRunner runner = new DummyDataRunner(
                dummyDataService,
                dummyDataProperties,
                benchmarkDataProperties,
                syncService,
                paymentRepository
        );

        runner.run(null);

        verify(dummyDataService).generateIfEmpty();
        verify(syncService, never()).syncTo(org.mockito.ArgumentMatchers.any());
    }
}
