package com.example.stealthpay.config;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SweepPropertiesTest {

    @Test
    void parsesSpendingPrivateKey() {
        assertEquals(new BigInteger("abcdef", 16), new SweepProperties("0xabcdef").spendingKey());
    }

    @Test
    void blankKeyFailsOnlyWhenUsed() {
        SweepProperties properties = new SweepProperties("");

        assertThrows(IllegalStateException.class, properties::spendingKey);
    }

    @Test
    void missingKeyFailsOnlyWhenUsed() {
        SweepProperties properties = new SweepProperties(null);

        assertThrows(IllegalStateException.class, properties::spendingKey);
    }

    @Test
    void toStringHidesSpendingPrivateKey() {
        assertFalse(new SweepProperties("0xabcdef").toString().contains("abcdef"));
    }
}
