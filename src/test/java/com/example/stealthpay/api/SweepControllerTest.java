package com.example.stealthpay.api;

import com.example.stealthpay.sweep.SweepService;
import com.example.stealthpay.sweep.SweptPayment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Also covers ApiExceptionHandler: each exception type maps to its status code. */
@WebMvcTest(SweepController.class)
@ActiveProfiles("test")
class SweepControllerTest {

    private static final String DESTINATION = "0x7e5f4552091a69125d5dfcb7b8c2659029395bdf";
    private static final String STEALTH_ADDRESS = "0x2b5ad5c4795c026514f8317c7a215e218dccd6cf";
    private static final String SWEEP_TX = "0x" + "cd".repeat(32);

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    SweepService sweepService;

    @Test
    void sweepsToDestinationAndReturnsTheSweepTransactions() throws Exception {
        when(sweepService.sweepAll(DESTINATION))
                .thenReturn(List.of(new SweptPayment(STEALTH_ADDRESS, SWEEP_TX, BigInteger.valueOf(790))));

        mockMvc.perform(sweep("{\"destination\": \"" + DESTINATION + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].stealthAddress").value(STEALTH_ADDRESS))
                .andExpect(jsonPath("$[0].txHash").value(SWEEP_TX))
                .andExpect(jsonPath("$[0].amountWei").value("790"));
    }

    @Test
    void blankDestinationIsBadRequest() throws Exception {
        mockMvc.perform(sweep("{\"destination\": \"\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(sweepService);
    }

    @Test
    void missingDestinationIsBadRequest() throws Exception {
        mockMvc.perform(sweep("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(sweepService);
    }

    @Test
    void malformedDestinationIsBadRequest() throws Exception {
        mockMvc.perform(sweep("{\"destination\": \"0x1234\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(sweepService);
    }

    @Test
    void illegalArgumentFromServiceIsBadRequest() throws Exception {
        when(sweepService.sweepAll(any())).thenThrow(new IllegalArgumentException("Not a valid Ethereum address"));

        mockMvc.perform(sweep("{\"destination\": \"" + DESTINATION + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Not a valid Ethereum address"));
    }

    @Test
    void illegalStateFromServiceIsServerError() throws Exception {
        when(sweepService.sweepAll(any())).thenThrow(new IllegalStateException("spending key is not set"));

        mockMvc.perform(sweep("{\"destination\": \"" + DESTINATION + "\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("spending key is not set"));
    }

    @Test
    void nodeFailureIsBadGateway() throws Exception {
        when(sweepService.sweepAll(any())).thenThrow(new IOException("connection refused"));

        mockMvc.perform(sweep("{\"destination\": \"" + DESTINATION + "\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("Ethereum node error: connection refused"));
    }

    private static MockHttpServletRequestBuilder sweep(String json) {
        return post("/api/sweeps").contentType(MediaType.APPLICATION_JSON).content(json);
    }
}
