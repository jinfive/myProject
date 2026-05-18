package com.example.myproject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.myproject.domain.batch.SettlementStrategy;
import com.example.myproject.domain.merchant.Merchant;
import com.example.myproject.domain.payment.Payment;
import com.example.myproject.domain.payment.PaymentStatus;
import com.example.myproject.domain.payment.PaymentType;
import com.example.myproject.repository.BatchJobHistoryRepository;
import com.example.myproject.repository.MerchantRepository;
import com.example.myproject.repository.PaymentRepository;
import com.example.myproject.repository.SettlementRepository;
import com.example.myproject.service.BasicLoopSettlementService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SettlementControllerCorsIntegrationTests {

    private static final LocalDate SETTLEMENT_DATE = LocalDate.of(2026, 5, 8);
    private static final String LOCAL_VITE_ORIGIN = "http://localhost:5173";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BasicLoopSettlementService settlementService;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private SettlementRepository settlementRepository;

    @Autowired
    private BatchJobHistoryRepository batchJobHistoryRepository;

    @BeforeEach
    void setUp() {
        batchJobHistoryRepository.deleteAll();
        settlementRepository.deleteAll();
        paymentRepository.deleteAll();
        merchantRepository.deleteAll();
    }

    @Test
    void deleteSettlementsPreflightAllowsLocalViteOriginAndDeleteMethod() throws Exception {
        mockMvc.perform(options("/api/settlements")
                        .param("date", SETTLEMENT_DATE.toString())
                        .header(HttpHeaders.ORIGIN, LOCAL_VITE_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "DELETE"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, LOCAL_VITE_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("DELETE")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("OPTIONS")));
    }

    @Test
    void deleteSettlementsResponseContainsCorsHeaderAndKeepsSourceDataAndHistories() throws Exception {
        createPaymentFixture();
        settlementService.run(SETTLEMENT_DATE, SettlementStrategy.BASIC_LOOP);

        assertThat(settlementRepository.count()).isEqualTo(1);
        assertThat(batchJobHistoryRepository.count()).isEqualTo(1);
        assertThat(paymentRepository.count()).isEqualTo(2);
        assertThat(merchantRepository.count()).isEqualTo(1);

        mockMvc.perform(delete("/api/settlements")
                        .param("date", SETTLEMENT_DATE.toString())
                        .header(HttpHeaders.ORIGIN, LOCAL_VITE_ORIGIN))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, LOCAL_VITE_ORIGIN))
                .andExpect(jsonPath("$.date").value(SETTLEMENT_DATE.toString()))
                .andExpect(jsonPath("$.deletedCount").value(1));

        assertThat(settlementRepository.count()).isZero();
        assertThat(batchJobHistoryRepository.count()).isEqualTo(1);
        assertThat(paymentRepository.count()).isEqualTo(2);
        assertThat(merchantRepository.count()).isEqualTo(1);
    }

    private void createPaymentFixture() {
        Merchant merchant = merchantRepository.save(new Merchant("merchant-a", new BigDecimal("0.0300")));
        paymentRepository.save(new Payment(
                merchant,
                PaymentType.PAYMENT,
                PaymentStatus.COMPLETED,
                new BigDecimal("10000.00"),
                SETTLEMENT_DATE,
                LocalDateTime.now()
        ));
        paymentRepository.save(new Payment(
                merchant,
                PaymentType.CANCEL,
                PaymentStatus.COMPLETED,
                new BigDecimal("1000.00"),
                SETTLEMENT_DATE,
                LocalDateTime.now()
        ));
    }
}
