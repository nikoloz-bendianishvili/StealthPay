package com.example.stealthpay.config;

import com.example.stealthpay.crypto.Secp256k1;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScannerPropertiesTest {

    /** 2·G, compressed. */
    private static final String K_HEX = "0x02c6047f9441ed7d6d3045406e95c07cd85c778e4b8cef3ca7abac09b95c709ee5";
    private static final String V_PRIVATE_HEX = "0x1f2e3d4c";

    @Test
    void parsesViewingPrivateKeyAsNumber() {
        ScannerProperties properties = new ScannerProperties(V_PRIVATE_HEX, K_HEX, 100, 12, 500);

        assertEquals(new BigInteger("1f2e3d4c", 16), properties.viewingKey());
    }

    @Test
    void parsesViewingPrivateKeyWithoutPrefix() {
        ScannerProperties properties = new ScannerProperties("1f2e3d4c", K_HEX, 100, 12, 500);

        assertEquals(new BigInteger("1f2e3d4c", 16), properties.viewingKey());
    }

    @Test
    void parsesSpendingPublicKeyAsCurvePoint() {
        ScannerProperties properties = new ScannerProperties(V_PRIVATE_HEX, K_HEX, 100, 12, 500);

        assertEquals(Secp256k1.multiplyG(BigInteger.TWO), properties.spendingKey());
    }

    @Test
    void rejectsMissingKeys() {
        assertThrows(IllegalArgumentException.class, () -> new ScannerProperties(null, K_HEX, 100, 12, 500));
        assertThrows(IllegalArgumentException.class, () -> new ScannerProperties(" ", K_HEX, 100, 12, 500));
        assertThrows(IllegalArgumentException.class, () -> new ScannerProperties(V_PRIVATE_HEX, "", 100, 12, 500));
    }

    @Test
    void rejectsNegativeStartBlockAndConfirmations() {
        assertThrows(IllegalArgumentException.class, () -> new ScannerProperties(V_PRIVATE_HEX, K_HEX, -1, 12, 500));
        assertThrows(IllegalArgumentException.class, () -> new ScannerProperties(V_PRIVATE_HEX, K_HEX, 100, -1, 500));
    }

    @Test
    void rejectsBatchSizeBelowOne() {
        assertThrows(IllegalArgumentException.class, () -> new ScannerProperties(V_PRIVATE_HEX, K_HEX, 100, 12, 0));
    }

    @Test
    void toStringHidesViewingPrivateKey() {
        ScannerProperties properties = new ScannerProperties(V_PRIVATE_HEX, K_HEX, 100, 12, 500);

        assertFalse(properties.toString().contains("1f2e3d4c"));
    }
}
