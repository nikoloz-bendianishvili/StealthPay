package com.example.stealthpay.crypto;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EthereumAddressTest {

    @Test
    void privateKeyOneGivesKnownAddress() {
        String address = EthereumAddress.fromPublicKey(Secp256k1.publicKey(BigInteger.ONE, false));

        assertEquals("0x7e5f4552091a69125d5dfcb7b8c2659029395bdf", address);
    }

    @Test
    void privateKeyTwoGivesKnownAddress() {
        String address = EthereumAddress.fromPublicKey(Secp256k1.publicKey(BigInteger.TWO, false));

        assertEquals("0x2b5ad5c4795c026514f8317c7a215e218dccd6cf", address);
    }

    @Test
    void privateKeyThreeGivesKnownAddress() {
        String address = EthereumAddress.fromPublicKey(Secp256k1.publicKey(BigInteger.valueOf(3), false));

        assertEquals("0x6813eb9362372eef6200f3b1dbc3f819671cba69", address);
    }

    @Test
    void addressIs42CharactersStartingWith0x() {
        String address = EthereumAddress.fromPublicKey(Secp256k1.publicKey(new BigInteger("123456789"), false));

        assertEquals(42, address.length());
        assertEquals("0x", address.substring(0, 2));
    }

    @Test
    void publicKeyWithWrongLengthIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> EthereumAddress.fromPublicKey(new byte[64]));
    }

    @Test
    void publicKeyWithoutPrefix04IsRejected() {
        byte[] publicKey = Secp256k1.publicKey(BigInteger.ONE, false);
        publicKey[0] = 0x02;

        assertThrows(IllegalArgumentException.class, () -> EthereumAddress.fromPublicKey(publicKey));
    }
}
