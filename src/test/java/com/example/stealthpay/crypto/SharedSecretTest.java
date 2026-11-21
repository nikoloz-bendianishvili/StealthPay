package com.example.stealthpay.crypto;

import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

class SharedSecretTest {

    private static final String HASH_OF_COMPRESSED_G =
            "aa61b794ba668ff67c7b8d037034adcca06964c0dffdb3eae70e8666faa5a63d";
    private static final String HASH_OF_UNCOMPRESSED_G =
            "393a75c54f3552ba0c8900297d6e99bb8abf8cc013bb0e912d0b176596fe7b88";

    @Test
    void hashOfGeneratorPointMatchesKnownValue() {
        byte[] hash = SharedSecret.hash(Secp256k1.multiplyG(BigInteger.ONE));

        assertEquals(HASH_OF_COMPRESSED_G, Hex.toHexString(hash));
    }

    @Test
    void hashUsesCompressedEncodingNotUncompressed() {
        byte[] hash = SharedSecret.hash(Secp256k1.multiplyG(BigInteger.ONE));

        assertNotEquals(HASH_OF_UNCOMPRESSED_G, Hex.toHexString(hash));
    }

    @Test
    void hashIsKeccakOfCompressedPoint() {
        ECPoint point = Secp256k1.multiplyG(Secp256k1.generatePrivateKey());

        assertArrayEquals(Keccak.hash(point.getEncoded(true)), SharedSecret.hash(point));
    }

    @Test
    void hashIs32Bytes() {
        byte[] hash = SharedSecret.hash(Secp256k1.multiplyG(Secp256k1.generatePrivateKey()));

        assertEquals(32, hash.length);
    }

    @Test
    void viewTagOfGeneratorPointHashMatchesKnownValue() {
        byte[] hash = Hex.decode(HASH_OF_COMPRESSED_G);

        assertEquals(0xaa, SharedSecret.viewTag(hash));
    }

    @Test
    void viewTagMatchesReferenceSdkExample() {
        byte[] hash = Hex.decode("158ce29a3dd0c8dca524e5776c2ba6361c280e013f87eee5eb799a713a939501");

        assertEquals(0x15, SharedSecret.viewTag(hash));
    }

    @Test
    void viewTagIsFirstByteAsNumberFrom0To255() {
        byte[] hash = new byte[32];

        hash[0] = 0x00;
        assertEquals(0, SharedSecret.viewTag(hash));

        hash[0] = 0x7f;
        assertEquals(127, SharedSecret.viewTag(hash));

        hash[0] = (byte) 0x80;
        assertEquals(128, SharedSecret.viewTag(hash));

        hash[0] = (byte) 0xff;
        assertEquals(255, SharedSecret.viewTag(hash));
    }

    @Test
    void viewTagRejectsWrongLength() {
        assertThrows(IllegalArgumentException.class, () -> SharedSecret.viewTag(new byte[31]));
        assertThrows(IllegalArgumentException.class, () -> SharedSecret.viewTag(new byte[33]));
        assertThrows(IllegalArgumentException.class, () -> SharedSecret.viewTag(new byte[0]));
    }
}
