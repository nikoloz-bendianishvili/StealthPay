package com.example.stealthpay.api;

import com.example.stealthpay.chain.SentPayment;
import com.example.stealthpay.chain.StealthPaymentSender;
import com.example.stealthpay.crypto.Announcement;
import com.example.stealthpay.crypto.MetaAddress;
import com.example.stealthpay.crypto.Secp256k1;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SendController.class)
@ActiveProfiles("test")
class SendControllerTest {

    private static final MetaAddress META_ADDRESS =
            new MetaAddress(Secp256k1.multiplyG(BigInteger.TWO), Secp256k1.multiplyG(BigInteger.valueOf(3)));
    private static final String STEALTH_ADDRESS = "0x2b5ad5c4795c026514f8317c7a215e218dccd6cf";
    private static final String TRANSFER_TX = "0x" + "11".repeat(32);
    private static final String ANNOUNCE_TX = "0x" + "22".repeat(32);

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    StealthPaymentSender sender;

    @Test
    void sendsToTheMetaAddressAndReturnsBothTransactions() throws Exception {
        Announcement announcement = new Announcement(STEALTH_ADDRESS, Secp256k1.multiplyG(BigInteger.valueOf(7)), 0x12);
        when(sender.send(META_ADDRESS, new BigInteger("1000000000000000")))
                .thenReturn(new SentPayment(TRANSFER_TX, ANNOUNCE_TX, announcement));

        mockMvc.perform(send(META_ADDRESS.encode(), "1000000000000000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stealthAddress").value(STEALTH_ADDRESS))
                .andExpect(jsonPath("$.transferTxHash").value(TRANSFER_TX))
                .andExpect(jsonPath("$.announceTxHash").value(ANNOUNCE_TX));

        verify(sender).send(META_ADDRESS, new BigInteger("1000000000000000"));
    }

    @Test
    void acceptsAmountLargerThanALong() throws Exception {
        String hugeAmount = "1" + "0".repeat(30);
        when(sender.send(any(), eq(new BigInteger(hugeAmount))))
                .thenReturn(new SentPayment(TRANSFER_TX, ANNOUNCE_TX,
                        new Announcement(STEALTH_ADDRESS, Secp256k1.multiplyG(BigInteger.valueOf(7)), 0x12)));

        mockMvc.perform(send(META_ADDRESS.encode(), hugeAmount))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsZeroAmount() throws Exception {
        expectBadRequest(send(META_ADDRESS.encode(), "0"));
    }

    @Test
    void rejectsNegativeAmount() throws Exception {
        expectBadRequest(send(META_ADDRESS.encode(), "-1"));
    }

    @Test
    void rejectsFractionalAmount() throws Exception {
        expectBadRequest(send(META_ADDRESS.encode(), "1.5"));
    }

    @Test
    void rejectsAmountWithMoreThan78Digits() throws Exception {
        expectBadRequest(send(META_ADDRESS.encode(), "1" + "0".repeat(78)));
    }

    @Test
    void rejectsMalformedMetaAddress() throws Exception {
        expectBadRequest(send("st:eth:0x1234", "1000"));
    }

    @Test
    void rejectsMetaAddressWhoseKeysAreNotCurvePoints() throws Exception {
        // Right length and hex, but 0x05 is not a valid compressed-key prefix, so MetaAddress.parse throws.
        String notOnCurve = "st:eth:0x05" + "00".repeat(32) + "05" + "00".repeat(32);

        expectBadRequest(send(notOnCurve, "1000"));
    }

    @Test
    void nodeFailureIsBadGateway() throws Exception {
        when(sender.send(any(), any())).thenThrow(new IOException("connection refused"));

        mockMvc.perform(send(META_ADDRESS.encode(), "1000"))
                .andExpect(status().isBadGateway());
    }

    private void expectBadRequest(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request).andExpect(status().isBadRequest());
        verifyNoInteractions(sender);
    }

    private static MockHttpServletRequestBuilder send(String metaAddress, String amountWei) {
        String json = "{\"metaAddress\": \"" + metaAddress + "\", \"amountWei\": \"" + amountWei + "\"}";
        return post("/api/payments/send").contentType(MediaType.APPLICATION_JSON).content(json);
    }
}
