package com.example.stealthpay.crypto;

import org.bouncycastle.util.encoders.Hex;

import java.util.Arrays;

public class EthereumAddress {

    /**
     * Ethereum address of a public key: the last 20 bytes of keccak256(x || y), as lowercase "0x" + 40 hex.
     * Expects the 65-byte uncompressed key. Its 0x04 prefix is only a format marker, so it is not hashed.
     */
    public static String fromPublicKey(byte[] uncompressedPublicKey) {
        if (uncompressedPublicKey.length != 65 || uncompressedPublicKey[0] != 0x04) {
            throw new IllegalArgumentException("Uncompressed public key should be 65 bytes size and should start with 0x04");
        }

        byte[] publicKeyWithoutPrefix = Arrays.copyOfRange(uncompressedPublicKey, 1, 65);
        byte[] keccakHash = Keccak.hash(publicKeyWithoutPrefix);
        byte[] addressBytes = Arrays.copyOfRange(keccakHash, 12, 32);
        return "0x" + Hex.toHexString(addressBytes);
    }
}
