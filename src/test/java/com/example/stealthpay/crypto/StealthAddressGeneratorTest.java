package com.example.stealthpay.crypto;

import org.bouncycastle.math.ec.ECPoint;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

class StealthAddressGeneratorTest {

    private static final BigInteger N =
            new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141", 16);

    private static final BigInteger SPENDING_PRIVATE_KEY = new BigInteger("1234567");
    private static final BigInteger VIEWING_PRIVATE_KEY = new BigInteger("7654321");
    private static final BigInteger EPHEMERAL_PRIVATE_KEY = new BigInteger("42");

    private static final MetaAddress META_ADDRESS = new MetaAddress(
            Secp256k1.multiplyG(SPENDING_PRIVATE_KEY),
            Secp256k1.multiplyG(VIEWING_PRIVATE_KEY)
    );

    @Test
    void sameEphemeralKeyGivesSameResult() {
        Announcement first = StealthAddressGenerator.generate(META_ADDRESS, EPHEMERAL_PRIVATE_KEY);
        Announcement second = StealthAddressGenerator.generate(META_ADDRESS, EPHEMERAL_PRIVATE_KEY);

        assertEquals(first, second);
    }

    @Test
    void ephemeralPublicKeyIsEphemeralPrivateKeyTimesG() {
        Announcement result = StealthAddressGenerator.generate(META_ADDRESS, EPHEMERAL_PRIVATE_KEY);

        assertEquals(Secp256k1.multiplyG(EPHEMERAL_PRIVATE_KEY), result.ephemeralPublicKey());
    }

    @Test
    void stealthAddressIs42CharactersStartingWith0x() {
        Announcement result = StealthAddressGenerator.generate(META_ADDRESS, EPHEMERAL_PRIVATE_KEY);

        assertEquals(42, result.stealthAddress().length());
        assertEquals("0x", result.stealthAddress().substring(0, 2));
    }

    @Test
    void viewTagIsBetween0And255() {
        for (int i = 0; i < 100; i++) {
            Announcement result = StealthAddressGenerator.generate(META_ADDRESS);

            assertTrue(result.viewTag() >= 0 && result.viewTag() <= 255);
        }
    }

    @Test
    void recipientComputesSameViewTag() {
        Announcement result = StealthAddressGenerator.generate(META_ADDRESS, EPHEMERAL_PRIVATE_KEY);

        byte[] s = recipientHashedSharedSecret(result.ephemeralPublicKey());

        assertEquals(s[0] & 0xFF, result.viewTag());
    }

    @Test
    void recipientCanDeriveStealthPrivateKeyForStealthAddress() {
        Announcement result = StealthAddressGenerator.generate(META_ADDRESS, EPHEMERAL_PRIVATE_KEY);

        String recipientAddress = recipientStealthAddress(result.ephemeralPublicKey());

        assertEquals(recipientAddress, result.stealthAddress());
    }

    @Test
    void recipientCanDeriveStealthPrivateKeyForRandomPayments() {
        for (int i = 0; i < 20; i++) {
            Announcement result = StealthAddressGenerator.generate(META_ADDRESS);

            assertEquals(recipientStealthAddress(result.ephemeralPublicKey()), result.stealthAddress());
        }
    }

    @Test
    void stealthAddressIsNotTheSpendingAddress() {
        Announcement result = StealthAddressGenerator.generate(META_ADDRESS, EPHEMERAL_PRIVATE_KEY);
        String spendingAddress = EthereumAddress.fromPublicKey(Secp256k1.publicKey(SPENDING_PRIVATE_KEY, false));

        assertNotEquals(spendingAddress, result.stealthAddress());
    }

    @Test
    void differentEphemeralKeysGiveDifferentStealthAddresses() {
        Announcement first = StealthAddressGenerator.generate(META_ADDRESS, BigInteger.valueOf(42));
        Announcement second = StealthAddressGenerator.generate(META_ADDRESS, BigInteger.valueOf(43));

        assertNotEquals(first.stealthAddress(), second.stealthAddress());
        assertNotEquals(first.ephemeralPublicKey(), second.ephemeralPublicKey());
    }

    @Test
    void twoRandomPaymentsToSameMetaAddressGiveDifferentStealthAddresses() {
        Announcement first = StealthAddressGenerator.generate(META_ADDRESS);
        Announcement second = StealthAddressGenerator.generate(META_ADDRESS);

        assertNotEquals(first.stealthAddress(), second.stealthAddress());
    }

    @Test
    void differentViewingKeyGivesDifferentStealthAddress() {
        MetaAddress otherMetaAddress = new MetaAddress(
                Secp256k1.multiplyG(SPENDING_PRIVATE_KEY),
                Secp256k1.multiplyG(VIEWING_PRIVATE_KEY.add(BigInteger.ONE))
        );

        Announcement first = StealthAddressGenerator.generate(META_ADDRESS, EPHEMERAL_PRIVATE_KEY);
        Announcement second = StealthAddressGenerator.generate(otherMetaAddress, EPHEMERAL_PRIVATE_KEY);

        assertNotEquals(first.stealthAddress(), second.stealthAddress());
    }

    @Test
    void invalidEphemeralPrivateKeyIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> StealthAddressGenerator.generate(META_ADDRESS, BigInteger.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> StealthAddressGenerator.generate(META_ADDRESS, N));
    }

    private static byte[] recipientHashedSharedSecret(ECPoint ephemeralPublicKey) {
        ECPoint sharedSecret = Secp256k1.multiply(ephemeralPublicKey, VIEWING_PRIVATE_KEY);
        return Keccak.hash(sharedSecret.getEncoded(true));
    }

    private static String recipientStealthAddress(ECPoint ephemeralPublicKey) {
        BigInteger s = new BigInteger(1, recipientHashedSharedSecret(ephemeralPublicKey));
        BigInteger stealthPrivateKey = SPENDING_PRIVATE_KEY.add(s).mod(N);
        return EthereumAddress.fromPublicKey(Secp256k1.publicKey(stealthPrivateKey, false));
    }
}
