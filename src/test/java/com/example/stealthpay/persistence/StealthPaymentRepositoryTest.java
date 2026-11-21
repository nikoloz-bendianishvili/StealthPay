package com.example.stealthpay.persistence;

import com.example.stealthpay.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test against a real Postgres, with the schema created by Flyway V1.
 * It proves the SQL constraints and NUMERIC(78,0) behave as the entities expect.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class StealthPaymentRepositoryTest {

    private static final String TX_HASH = "0x" + "ab".repeat(32);

    @Autowired
    StealthPaymentRepository paymentRepository;

    @Autowired
    ScannerStateRepository scannerStateRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        paymentRepository.deleteAll();
        scannerStateRepository.deleteAll();
    }

    @Test
    void storesWeiAmountsTooBigForALong() {
        BigInteger huge = BigInteger.TWO.pow(200);
        StealthPayment saved = paymentRepository.save(payment(huge, 0));

        StealthPayment loaded = paymentRepository.findById(saved.getId()).orElseThrow();

        assertEquals(huge, loaded.getAmountWei());
    }

    @Test
    void rejectsTheSameTxHashAndLogIndexTwice() {
        paymentRepository.saveAndFlush(payment(BigInteger.TEN, 0));

        assertThrows(DataIntegrityViolationException.class,
                () -> paymentRepository.saveAndFlush(payment(BigInteger.TEN, 0)));
    }

    @Test
    void allowsTheSameTxHashWithAnotherLogIndex() {
        paymentRepository.saveAndFlush(payment(BigInteger.TEN, 0));
        paymentRepository.saveAndFlush(payment(BigInteger.TEN, 1));

        assertEquals(2, paymentRepository.count());
    }

    @Test
    void existsByTxHashAndLogIndex() {
        paymentRepository.save(payment(BigInteger.TEN, 4));

        assertTrue(paymentRepository.existsByTxHashAndLogIndex(TX_HASH, 4));
        assertFalse(paymentRepository.existsByTxHashAndLogIndex(TX_HASH, 5));
    }

    @Test
    void sumIsZeroWhenThereAreNoPayments() {
        assertEquals(BigInteger.ZERO, paymentRepository.sumAmountByStatus(PaymentStatus.UNSPENT));
    }

    @Test
    void sumCountsOnlyUnspentPayments() {
        paymentRepository.save(payment(BigInteger.valueOf(100), 0));
        paymentRepository.save(payment(BigInteger.valueOf(250), 1));
        StealthPayment swept = payment(BigInteger.valueOf(1_000), 2);
        swept.markSwept();
        paymentRepository.save(swept);

        assertEquals(BigInteger.valueOf(350), paymentRepository.sumAmountByStatus(PaymentStatus.UNSPENT));
        assertEquals(BigInteger.valueOf(1_000), paymentRepository.sumAmountByStatus(PaymentStatus.SWEPT));
    }

    @Test
    void findByStatusReturnsOnlyThatStatus() {
        paymentRepository.save(payment(BigInteger.ONE, 0));
        StealthPayment swept = payment(BigInteger.ONE, 1);
        swept.markSwept();
        paymentRepository.save(swept);

        assertEquals(1, paymentRepository.findByStatus(PaymentStatus.UNSPENT).size());
        assertEquals(1, paymentRepository.findByStatus(PaymentStatus.SWEPT).size());
    }

    @Test
    void scannerStateIsASingleRow() {
        scannerStateRepository.save(new ScannerState(100L));

        assertThrows(DataIntegrityViolationException.class,
                () -> jdbcTemplate.update("insert into scanner_state (id, last_block) values (2, 0)"));
    }

    @Test
    void scannerStateStartsOneBlockBeforeTheStartBlock() {
        scannerStateRepository.save(new ScannerState(100L));

        assertEquals(99L, scannerStateRepository.findById(1L).orElseThrow().getLastScannedBlock());
    }

    private static StealthPayment payment(BigInteger amountWei, int logIndex) {
        return new StealthPayment("0x7e5f4552091a69125d5dfcb7b8c2659029395bdf", "0x02" + "11".repeat(32),
                7, amountWei, TX_HASH, 95, logIndex);
    }
}
