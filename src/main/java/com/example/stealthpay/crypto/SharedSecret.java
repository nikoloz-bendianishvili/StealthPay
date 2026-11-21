package com.example.stealthpay.crypto;


import org.bouncycastle.math.ec.ECPoint;

public class SharedSecret {

    /** s = keccak256(S compressed). Sender and recipient get the same s, because r·V = v·R = S. */
    static byte[] hash(ECPoint sharedSecret) {
        return Keccak.hash(sharedSecret.getEncoded(true));
    }

    /** First byte of s as 0..255. Lets the scanner reject most other people's payments with one comparison. */
    static int viewTag(byte[] hashedSecret) {
        if (hashedSecret.length != 32) {
            throw new IllegalArgumentException("Hashed shared secret should be 32 bytes");
        }

        return hashedSecret[0] & 0xFF;
    }
}
