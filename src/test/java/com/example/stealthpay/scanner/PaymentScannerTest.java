package com.example.stealthpay.scanner;

import com.example.stealthpay.TestcontainersConfiguration;
import com.example.stealthpay.chain.AnnouncementLog;
import com.example.stealthpay.chain.AnnouncementReader;
import com.example.stealthpay.chain.BalanceReader;
import com.example.stealthpay.crypto.Announcement;
import com.example.stealthpay.crypto.MetaAddress;
import com.example.stealthpay.crypto.Secp256k1;
import com.example.stealthpay.crypto.StealthAddressGenerator;
import com.example.stealthpay.persistence.PaymentStatus;
import com.example.stealthpay.persistence.ScannerStateRepository;
import com.example.stealthpay.persistence.StealthPayment;
import com.example.stealthpay.persistence.StealthPaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Real Postgres (Testcontainers), real crypto, real Spring wiring. Only the two classes that talk to
 * the Sepolia node are mocked. The test profile sets start-block 90, confirmations 12, batch size 50.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class PaymentScannerTest {

    /** Matches the test profile: K = 2·G, V = 3·G. */
    private static final MetaAddress OUR_META_ADDRESS =
            new MetaAddress(Secp256k1.multiplyG(BigInteger.TWO), Secp256k1.multiplyG(BigInteger.valueOf(3)));
    private static final MetaAddress SOMEONE_ELSES_META_ADDRESS =
            new MetaAddress(Secp256k1.multiplyG(BigInteger.valueOf(5)), Secp256k1.multiplyG(BigInteger.valueOf(7)));

    private static final BigInteger AMOUNT = new BigInteger("1000000000000000"); // 0.001 ETH

    @MockitoBean
    AnnouncementReader announcementReader;

    @MockitoBean
    BalanceReader balanceReader;

    @Autowired
    PaymentScanner scanner;

    @Autowired
    StealthPaymentRepository paymentRepository;

    @Autowired
    ScannerStateRepository scannerStateRepository;

    @BeforeEach
    void cleanDatabase() throws IOException {
        paymentRepository.deleteAll();
        scannerStateRepository.deleteAll();
        when(balanceReader.balance(anyString())).thenReturn(AMOUNT);
    }

    @Test
    void savesOurPaymentAndAdvancesLastScannedBlock() throws IOException {
        AnnouncementLog ours = announcementLog(OUR_META_ADDRESS, 95);
        when(announcementReader.safeHeadBlock(12)).thenReturn(100L);
        when(announcementReader.read(90, 100)).thenReturn(List.of(ours));

        scanner.scan();

        List<StealthPayment> payments = paymentRepository.findAll();
        assertEquals(1, payments.size());
        StealthPayment payment = payments.getFirst();
        assertEquals(ours.stealthAddress(), payment.getStealthAddress());
        assertEquals(Numeric.toHexString(ours.ephemeralPublicKey().getEncoded(true)), payment.getEphemeralPublicKey());
        assertEquals(ours.viewTag(), payment.getViewTag());
        assertEquals(AMOUNT, payment.getAmountWei());
        assertEquals(ours.txHash(), payment.getTxHash());
        assertEquals(95L, payment.getBlockNumber());
        assertEquals(PaymentStatus.UNSPENT, payment.getStatus());
        assertEquals(100L, lastScannedBlock());
    }

    @Test
    void ignoresSomeoneElsesPayment() throws IOException {
        when(announcementReader.safeHeadBlock(12)).thenReturn(100L);
        when(announcementReader.read(90, 100)).thenReturn(List.of(announcementLog(SOMEONE_ELSES_META_ADDRESS, 95)));

        scanner.scan();

        assertEquals(0, paymentRepository.count());
        assertEquals(100L, lastScannedBlock());
    }

    @Test
    void doesNotSaveTheSameAnnouncementTwice() throws IOException {
        AnnouncementLog ours = announcementLog(OUR_META_ADDRESS, 95);
        when(announcementReader.safeHeadBlock(12)).thenReturn(100L);
        when(announcementReader.read(90, 100)).thenReturn(List.of(ours));

        scanner.scan();
        scannerStateRepository.deleteAll(); // forget progress, so the same blocks are scanned again
        scanner.scan();

        assertEquals(1, paymentRepository.count());
    }

    @Test
    void nextScanContinuesAfterLastScannedBlock() throws IOException {
        when(announcementReader.safeHeadBlock(12)).thenReturn(100L, 120L);
        when(announcementReader.read(anyLong(), anyLong())).thenReturn(List.of());

        scanner.scan();
        scanner.scan();

        verify(announcementReader).read(90, 100);
        verify(announcementReader).read(101, 120);
        assertEquals(120L, lastScannedBlock());
    }

    @Test
    void scansAtMostOneBatchPerRun() throws IOException {
        when(announcementReader.safeHeadBlock(12)).thenReturn(1_000L);
        when(announcementReader.read(anyLong(), anyLong())).thenReturn(List.of());

        scanner.scan();

        verify(announcementReader).read(90, 139); // 50 blocks: 90..139
        assertEquals(139L, lastScannedBlock());
    }

    @Test
    void doesNothingWhenThereAreNoNewSafeBlocks() throws IOException {
        when(announcementReader.safeHeadBlock(12)).thenReturn(89L); // start block 90 isn't safe yet

        scanner.scan();

        verify(announcementReader, never()).read(anyLong(), anyLong());
        assertEquals(0, scannerStateRepository.count());
    }

    private long lastScannedBlock() {
        return scannerStateRepository.findById(1L).orElseThrow().getLastScannedBlock();
    }

    /** What AnnouncementReader would return for a real payment to this meta-address. */
    private static AnnouncementLog announcementLog(MetaAddress recipient, long blockNumber) {
        Announcement announcement = StealthAddressGenerator.generate(recipient);
        return new AnnouncementLog(
                announcement.stealthAddress(),
                announcement.ephemeralPublicKey(),
                announcement.viewTag(),
                "0x" + "ab".repeat(32),
                blockNumber,
                3);
    }
}
