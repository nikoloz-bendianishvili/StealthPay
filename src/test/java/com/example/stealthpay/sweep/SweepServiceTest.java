package com.example.stealthpay.sweep;

import com.example.stealthpay.TestcontainersConfiguration;
import com.example.stealthpay.chain.StealthAddressSweeper;
import com.example.stealthpay.chain.SweepTransaction;
import com.example.stealthpay.crypto.Announcement;
import com.example.stealthpay.crypto.EthereumAddress;
import com.example.stealthpay.crypto.MetaAddress;
import com.example.stealthpay.crypto.Secp256k1;
import com.example.stealthpay.crypto.StealthAddressGenerator;
import com.example.stealthpay.persistence.PaymentStatus;
import com.example.stealthpay.persistence.StealthPayment;
import com.example.stealthpay.persistence.StealthPaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Real Postgres and real key derivation. Only the on-chain sweep is mocked. Test profile: k = 2, v = 3. */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class SweepServiceTest {

    private static final BigInteger SPENDING_PRIVATE_KEY = BigInteger.TWO;
    private static final BigInteger VIEWING_PRIVATE_KEY = BigInteger.valueOf(3);
    private static final MetaAddress OUR_META_ADDRESS =
            new MetaAddress(Secp256k1.multiplyG(SPENDING_PRIVATE_KEY), Secp256k1.multiplyG(VIEWING_PRIVATE_KEY));

    private static final String DESTINATION = "0x7e5f4552091a69125d5dfcb7b8c2659029395bdf";

    @MockitoBean
    StealthAddressSweeper sweeper;

    @Autowired
    SweepService sweepService;

    @Autowired
    StealthPaymentRepository paymentRepository;

    @BeforeEach
    void cleanDatabase() {
        paymentRepository.deleteAll();
    }

    @Test
    void sweepsWithTheKeyThatControlsTheStealthAddressAndMarksItSwept() throws Exception {
        StealthPayment payment = paymentRepository.save(paymentTo(OUR_META_ADDRESS));
        when(sweeper.sweep(any(), eq(DESTINATION)))
                .thenReturn(Optional.of(new SweepTransaction("0x" + "cd".repeat(32), BigInteger.valueOf(900))));

        List<SweptPayment> swept = sweepService.sweepAll(DESTINATION);

        ArgumentCaptor<BigInteger> usedKey = ArgumentCaptor.forClass(BigInteger.class);
        verify(sweeper).sweep(usedKey.capture(), eq(DESTINATION));
        assertEquals(payment.getStealthAddress(), addressOf(usedKey.getValue()));

        assertEquals(List.of(new SweptPayment(payment.getStealthAddress(), "0x" + "cd".repeat(32), BigInteger.valueOf(900))), swept);
        assertEquals(PaymentStatus.SWEPT, paymentRepository.findById(payment.getId()).orElseThrow().getStatus());
    }

    @Test
    void leavesPaymentUnspentWhenBalanceDoesNotCoverGas() throws Exception {
        StealthPayment payment = paymentRepository.save(paymentTo(OUR_META_ADDRESS));
        when(sweeper.sweep(any(), eq(DESTINATION))).thenReturn(Optional.empty());

        List<SweptPayment> swept = sweepService.sweepAll(DESTINATION);

        assertTrue(swept.isEmpty());
        assertEquals(PaymentStatus.UNSPENT, paymentRepository.findById(payment.getId()).orElseThrow().getStatus());
    }

    @Test
    void rejectsInvalidDestination() {
        assertThrows(IllegalArgumentException.class, () -> sweepService.sweepAll("not-an-address"));
        verifyNoInteractions(sweeper);
    }

    @Test
    void stealthPrivateKeyRefusesWrongSpendingKey() {
        StealthPayment payment = paymentTo(OUR_META_ADDRESS);

        assertThrows(IllegalStateException.class,
                () -> SweepService.stealthPrivateKey(payment, VIEWING_PRIVATE_KEY, BigInteger.valueOf(5)));
    }

    private static StealthPayment paymentTo(MetaAddress recipient) {
        Announcement announcement = StealthAddressGenerator.generate(recipient);
        return new StealthPayment(
                announcement.stealthAddress(),
                Numeric.toHexString(announcement.ephemeralPublicKey().getEncoded(true)),
                announcement.viewTag(),
                BigInteger.valueOf(1_000_000),
                "0x" + "ab".repeat(32),
                95,
                0);
    }

    private static String addressOf(BigInteger privateKey) {
        return EthereumAddress.fromPublicKey(Secp256k1.publicKey(privateKey, false));
    }
}
