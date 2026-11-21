package com.example.stealthpay.crypto;


import org.bouncycastle.crypto.digests.KeccakDigest;

public class Keccak {

    /**
     * Keccak-256 of the input (32 bytes). This is Ethereum's hash, and it differs from Java's SHA3-256,
     * which uses different padding.
     */
    public static byte[] hash(byte[] input) {
        KeccakDigest keccakDigest = new KeccakDigest(256);
        keccakDigest.update(input, 0, input.length);
        byte[] output = new byte[keccakDigest.getDigestSize()];
        keccakDigest.doFinal(output, 0);
        return output;
    }
}
