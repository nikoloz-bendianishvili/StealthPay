package com.example.stealthpay.api;

import com.example.stealthpay.config.ScannerProperties;
import com.example.stealthpay.crypto.MetaAddress;
import com.example.stealthpay.crypto.Secp256k1;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Real ScannerProperties from the test profile: v = 3, K = 2·G. */
@WebMvcTest(MetaAddressController.class)
@ActiveProfiles("test")
class MetaAddressControllerTest {

    /** A web slice doesn't load @ConfigurationProperties by itself, so register ScannerProperties here. */
    @TestConfiguration
    @EnableConfigurationProperties(ScannerProperties.class)
    static class Config {
    }

    @Autowired
    MockMvc mockMvc;

    @Test
    void returnsMetaAddressBuiltFromKAndV() throws Exception {
        String expected = new MetaAddress(
                Secp256k1.multiplyG(BigInteger.TWO),
                Secp256k1.multiplyG(BigInteger.valueOf(3))).encode();

        mockMvc.perform(get("/api/meta-address"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metaAddress").value(expected));
    }
}
