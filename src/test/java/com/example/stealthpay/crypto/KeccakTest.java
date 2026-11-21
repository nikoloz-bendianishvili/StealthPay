package com.example.stealthpay.crypto;

import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeccakTest {

    @Test
    void hashOfEmptyInputMatchesKnownValue() {
        byte[] hash = Keccak.hash(new byte[0]);
        assertEquals("c5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470", Hex.toHexString(hash));
    }

    @Test
    void hashOfThirtyTwoZeroBytesMatchesKnownValue() {
        byte[] hash = Keccak.hash(new byte[32]);
        assertEquals("290decd9548b62a8d60345a988386fc84ba6bc95484008f6362f93160ef3e563", Hex.toHexString(hash));
    }

    @Test
    void hashIsAlways32Bytes() {
        assertEquals(32, Keccak.hash(new byte[0]).length);
        assertEquals(32, Keccak.hash(new byte[1000]).length);
        assertEquals(32, Keccak.hash("stealthpay".getBytes(StandardCharsets.UTF_8)).length);
    }
}
