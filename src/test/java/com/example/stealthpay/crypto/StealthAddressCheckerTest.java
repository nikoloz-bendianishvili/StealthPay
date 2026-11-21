package com.example.stealthpay.crypto;

import org.bouncycastle.math.ec.ECPoint;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

class StealthAddressCheckerTest {

    private static final BigInteger SPENDING_PRIVATE_KEY = new BigInteger("1234567");
    private static final BigInteger VIEWING_PRIVATE_KEY = new BigInteger("7654321");
    private static final BigInteger EPHEMERAL_PRIVATE_KEY = new BigInteger("42");

    private static final ECPoint SPENDING_PUBLIC_KEY = Secp256k1.multiplyG(SPENDING_PRIVATE_KEY);
    private static final ECPoint VIEWING_PUBLIC_KEY = Secp256k1.multiplyG(VIEWING_PRIVATE_KEY);

    private static final MetaAddress META_ADDRESS = new MetaAddress(SPENDING_PUBLIC_KEY, VIEWING_PUBLIC_KEY);

    @Test
    void ownPaymentIsMine() {
        Announcement announcement = StealthAddressGenerator.generate(META_ADDRESS, EPHEMERAL_PRIVATE_KEY);

        assertTrue(StealthAddressChecker.isMine(announcement, VIEWING_PRIVATE_KEY, SPENDING_PUBLIC_KEY));
    }

    @Test
    void ownRandomPaymentsAreMine() {
        for (int i = 0; i < 20; i++) {
            Announcement announcement = StealthAddressGenerator.generate(META_ADDRESS);

            assertTrue(StealthAddressChecker.isMine(announcement, VIEWING_PRIVATE_KEY, SPENDING_PUBLIC_KEY));
        }
    }

    @Test
    void paymentToSomeoneElseIsNotMine() {
        MetaAddress someoneElse = new MetaAddress(
                Secp256k1.multiplyG(Secp256k1.generatePrivateKey()),
                Secp256k1.multiplyG(Secp256k1.generatePrivateKey())
        );

        for (int i = 0; i < 20; i++) {
            Announcement announcement = StealthAddressGenerator.generate(someoneElse);

            assertFalse(StealthAddressChecker.isMine(announcement, VIEWING_PRIVATE_KEY, SPENDING_PUBLIC_KEY));
        }
    }

    @Test
    void wrongViewingKeyIsNotMine() {
        Announcement announcement = StealthAddressGenerator.generate(META_ADDRESS, EPHEMERAL_PRIVATE_KEY);
        BigInteger otherViewingKey = VIEWING_PRIVATE_KEY.add(BigInteger.ONE);

        assertFalse(StealthAddressChecker.isMine(announcement, otherViewingKey, SPENDING_PUBLIC_KEY));
    }

    @Test
    void wrongSpendingKeyIsNotMineEvenThoughViewTagMatches() {
        Announcement announcement = StealthAddressGenerator.generate(META_ADDRESS, EPHEMERAL_PRIVATE_KEY);
        ECPoint otherSpendingKey = Secp256k1.multiplyG(SPENDING_PRIVATE_KEY.add(BigInteger.ONE));

        assertFalse(StealthAddressChecker.isMine(announcement, VIEWING_PRIVATE_KEY, otherSpendingKey));
    }

    @Test
    void wrongViewTagIsNotMine() {
        Announcement announcement = StealthAddressGenerator.generate(META_ADDRESS, EPHEMERAL_PRIVATE_KEY);
        Announcement wrongViewTag = new Announcement(
                announcement.stealthAddress(),
                announcement.ephemeralPublicKey(),
                (announcement.viewTag() + 1) % 256
        );

        assertFalse(StealthAddressChecker.isMine(wrongViewTag, VIEWING_PRIVATE_KEY, SPENDING_PUBLIC_KEY));
    }

    @Test
    void wrongStealthAddressWithCorrectViewTagIsNotMine() {
        Announcement announcement = StealthAddressGenerator.generate(META_ADDRESS, EPHEMERAL_PRIVATE_KEY);
        Announcement wrongAddress = new Announcement(
                "0x0000000000000000000000000000000000000000",
                announcement.ephemeralPublicKey(),
                announcement.viewTag()
        );

        assertFalse(StealthAddressChecker.isMine(wrongAddress, VIEWING_PRIVATE_KEY, SPENDING_PUBLIC_KEY));
    }

    @Test
    void stealthAddressInUpperCaseIsStillMine() {
        Announcement announcement = StealthAddressGenerator.generate(META_ADDRESS, EPHEMERAL_PRIVATE_KEY);
        Announcement upperCase = new Announcement(
                "0x" + announcement.stealthAddress().substring(2).toUpperCase(),
                announcement.ephemeralPublicKey(),
                announcement.viewTag()
        );

        assertTrue(StealthAddressChecker.isMine(upperCase, VIEWING_PRIVATE_KEY, SPENDING_PUBLIC_KEY));
    }

    @Test
    void stealthPrivateKeyControlsStealthAddress() {
        Announcement announcement = StealthAddressGenerator.generate(META_ADDRESS, EPHEMERAL_PRIVATE_KEY);

        BigInteger stealthPrivateKey = StealthAddressChecker.stealthPrivateKey(
                announcement.ephemeralPublicKey(), VIEWING_PRIVATE_KEY, SPENDING_PRIVATE_KEY);

        String address = EthereumAddress.fromPublicKey(Secp256k1.publicKey(stealthPrivateKey, false));
        assertEquals(announcement.stealthAddress(), address);
    }

    @Test
    void stealthPrivateKeyControlsRandomStealthAddresses() {
        for (int i = 0; i < 20; i++) {
            Announcement announcement = StealthAddressGenerator.generate(META_ADDRESS);

            BigInteger stealthPrivateKey = StealthAddressChecker.stealthPrivateKey(
                    announcement.ephemeralPublicKey(), VIEWING_PRIVATE_KEY, SPENDING_PRIVATE_KEY);

            String address = EthereumAddress.fromPublicKey(Secp256k1.publicKey(stealthPrivateKey, false));
            assertEquals(announcement.stealthAddress(), address);
        }
    }

    @Test
    void stealthPrivateKeyIsDifferentForEveryPayment() {
        Announcement first = StealthAddressGenerator.generate(META_ADDRESS, BigInteger.valueOf(42));
        Announcement second = StealthAddressGenerator.generate(META_ADDRESS, BigInteger.valueOf(43));

        BigInteger firstKey = StealthAddressChecker.stealthPrivateKey(
                first.ephemeralPublicKey(), VIEWING_PRIVATE_KEY, SPENDING_PRIVATE_KEY);
        BigInteger secondKey = StealthAddressChecker.stealthPrivateKey(
                second.ephemeralPublicKey(), VIEWING_PRIVATE_KEY, SPENDING_PRIVATE_KEY);

        assertNotEquals(firstKey, secondKey);
        assertNotEquals(SPENDING_PRIVATE_KEY, firstKey);
    }

    @Test
    void wrongSpendingPrivateKeyDoesNotControlStealthAddress() {
        Announcement announcement = StealthAddressGenerator.generate(META_ADDRESS, EPHEMERAL_PRIVATE_KEY);

        BigInteger wrongKey = StealthAddressChecker.stealthPrivateKey(
                announcement.ephemeralPublicKey(), VIEWING_PRIVATE_KEY, SPENDING_PRIVATE_KEY.add(BigInteger.ONE));

        String address = EthereumAddress.fromPublicKey(Secp256k1.publicKey(wrongKey, false));
        assertNotEquals(announcement.stealthAddress(), address);
    }
}
