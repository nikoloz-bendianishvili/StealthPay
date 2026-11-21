package com.example.stealthpay.api;

import com.example.stealthpay.TestcontainersConfiguration;
import com.example.stealthpay.chain.AnnouncementLog;
import com.example.stealthpay.chain.AnnouncementReader;
import com.example.stealthpay.chain.BalanceReader;
import com.example.stealthpay.chain.StealthAddressSweeper;
import com.example.stealthpay.chain.SweepTransaction;
import com.example.stealthpay.crypto.Announcement;
import com.example.stealthpay.crypto.EthereumAddress;
import com.example.stealthpay.crypto.MetaAddress;
import com.example.stealthpay.crypto.Secp256k1;
import com.example.stealthpay.crypto.StealthAddressGenerator;
import com.example.stealthpay.persistence.ScannerStateRepository;
import com.example.stealthpay.persistence.StealthPaymentRepository;
import com.example.stealthpay.scanner.PaymentScanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasToString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The whole recipient flow over HTTP: scan → balance → sweep → payments.
 * Everything is real (Spring, Postgres + Flyway, crypto, controllers, scanner, sweep service)
 * except the three beans that talk to the Sepolia node.
 * Test profile: k = 2, v = 3, start block 90, confirmations 12.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class StealthPayApiIntegrationTest {

    private static final MetaAddress OUR_META_ADDRESS =
            new MetaAddress(Secp256k1.multiplyG(BigInteger.TWO), Secp256k1.multiplyG(BigInteger.valueOf(3)));
    private static final BigInteger AMOUNT = new BigInteger("1000000000000000");
    private static final BigInteger SWEPT_AMOUNT = new BigInteger("790000000000000");
    private static final String DESTINATION = "0x7e5f4552091a69125d5dfcb7b8c2659029395bdf";
    private static final String SWEEP_TX = "0x" + "cd".repeat(32);

    @MockitoBean
    AnnouncementReader announcementReader;

    @MockitoBean
    BalanceReader balanceReader;

    @MockitoBean
    StealthAddressSweeper sweeper;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    PaymentScanner scanner;

    @Autowired
    StealthPaymentRepository paymentRepository;

    @Autowired
    ScannerStateRepository scannerStateRepository;

    @BeforeEach
    void cleanDatabase() {
        paymentRepository.deleteAll();
        scannerStateRepository.deleteAll();
    }

    @Test
    void metaAddressEndpointMatchesTheConfiguredKeys() throws Exception {
        mockMvc.perform(get("/api/meta-address"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metaAddress").value(OUR_META_ADDRESS.encode()));
    }

    @Test
    void foundPaymentShowsUpInBalanceAndCanBeSwept() throws Exception {
        // 1. A sender paid our meta-address; the scanner finds it on chain.
        AnnouncementLog ours = announcementTo(OUR_META_ADDRESS);
        when(announcementReader.safeHeadBlock(12)).thenReturn(100L);
        when(announcementReader.read(90, 100)).thenReturn(List.of(ours));
        when(balanceReader.balance(anyString())).thenReturn(AMOUNT);
        scanner.scan();

        // 2. It is visible over HTTP.
        mockMvc.perform(get("/api/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanceWei").value(hasToString(AMOUNT.toString())));
        mockMvc.perform(get("/api/payments").param("status", "UNSPENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].stealthAddress").value(ours.stealthAddress()));

        // 3. Sweep it.
        when(sweeper.sweep(any(), eq(DESTINATION))).thenReturn(Optional.of(new SweepTransaction(SWEEP_TX, SWEPT_AMOUNT)));
        mockMvc.perform(post("/api/sweeps").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destination\": \"" + DESTINATION + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].stealthAddress").value(ours.stealthAddress()))
                .andExpect(jsonPath("$[0].txHash").value(SWEEP_TX))
                .andExpect(jsonPath("$[0].amountWei").value(SWEPT_AMOUNT.toString()));

        // The sweep used the real derived key: (k + s) mod n must control the stealth address.
        ArgumentCaptor<BigInteger> stealthPrivateKey = ArgumentCaptor.forClass(BigInteger.class);
        verify(sweeper).sweep(stealthPrivateKey.capture(), eq(DESTINATION));
        assertEquals(ours.stealthAddress(),
                EthereumAddress.fromPublicKey(Secp256k1.publicKey(stealthPrivateKey.getValue(), false)));

        // 4. Afterwards: the payment is SWEPT and the balance is 0.
        mockMvc.perform(get("/api/payments").param("status", "SWEPT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanceWei").value(hasToString("0")));
    }

    @Test
    void someoneElsesPaymentNeverReachesTheApi() throws Exception {
        MetaAddress someoneElse =
                new MetaAddress(Secp256k1.multiplyG(BigInteger.valueOf(5)), Secp256k1.multiplyG(BigInteger.valueOf(7)));
        when(announcementReader.safeHeadBlock(12)).thenReturn(100L);
        when(announcementReader.read(90, 100)).thenReturn(List.of(announcementTo(someoneElse)));
        scanner.scan();

        mockMvc.perform(get("/api/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/balance"))
                .andExpect(jsonPath("$.balanceWei").value(hasToString("0")));
    }

    @Test
    void sweepWithNothingUnspentReturnsAnEmptyList() throws Exception {
        mockMvc.perform(post("/api/sweeps").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destination\": \"" + DESTINATION + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void invalidSweepRequestIsRejectedBeforeAnythingHappens() throws Exception {
        mockMvc.perform(post("/api/sweeps").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destination\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    private static AnnouncementLog announcementTo(MetaAddress recipient) {
        Announcement announcement = StealthAddressGenerator.generate(recipient);
        return new AnnouncementLog(announcement.stealthAddress(), announcement.ephemeralPublicKey(),
                announcement.viewTag(), "0x" + "ab".repeat(32), 95, 3);
    }
}
