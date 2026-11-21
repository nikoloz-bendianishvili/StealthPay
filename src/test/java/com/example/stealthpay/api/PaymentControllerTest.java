package com.example.stealthpay.api;

import com.example.stealthpay.persistence.PaymentStatus;
import com.example.stealthpay.persistence.StealthPayment;
import com.example.stealthpay.persistence.StealthPaymentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigInteger;
import java.util.List;

import static org.hamcrest.Matchers.hasToString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Amounts are checked with hasToString(...), so the tests pass whether wei is sent as a JSON number or a string.
 */
@WebMvcTest(PaymentController.class)
@ActiveProfiles("test")
class PaymentControllerTest {

    private static final String ADDRESS = "0x7e5f4552091a69125d5dfcb7b8c2659029395bdf";
    private static final String TX_HASH = "0x" + "ab".repeat(32);

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    StealthPaymentRepository paymentRepository;

    @Test
    void listsAllPaymentsWhenNoStatusIsGiven() throws Exception {
        when(paymentRepository.findAll()).thenReturn(List.of(payment()));

        mockMvc.perform(get("/api/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].stealthAddress").value(ADDRESS))
                .andExpect(jsonPath("$[0].amountWei").value(hasToString("1000")))
                .andExpect(jsonPath("$[0].txHash").value(TX_HASH))
                .andExpect(jsonPath("$[0].blockNumber").value(95))
                .andExpect(jsonPath("$[0].status").value("UNSPENT"));
    }

    @Test
    void filtersByStatus() throws Exception {
        when(paymentRepository.findByStatus(PaymentStatus.SWEPT)).thenReturn(List.of());

        mockMvc.perform(get("/api/payments").param("status", "SWEPT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(paymentRepository).findByStatus(PaymentStatus.SWEPT);
    }

    @Test
    void unknownStatusIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/payments").param("status", "FOO"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(paymentRepository);
    }

    @Test
    void balanceIsTheSumOfUnspentPayments() throws Exception {
        when(paymentRepository.sumAmountByStatus(PaymentStatus.UNSPENT)).thenReturn(BigInteger.valueOf(1500));

        mockMvc.perform(get("/api/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanceWei").value(hasToString("1500")));
    }

    private static StealthPayment payment() {
        return new StealthPayment(ADDRESS, "0x02" + "11".repeat(32), 7, BigInteger.valueOf(1000), TX_HASH, 95, 0);
    }
}
