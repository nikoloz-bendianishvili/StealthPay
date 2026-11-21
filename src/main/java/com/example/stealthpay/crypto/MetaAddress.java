package com.example.stealthpay.crypto;

import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.DecoderException;
import org.bouncycastle.util.encoders.Hex;

import java.util.Arrays;

public record MetaAddress(
        ECPoint spendingPublicKey,
        ECPoint viewingPublicKey
) {

    private static final String PREFIX = "st:eth:0x";
    private static final int KEY_LENGTH = 33;

    /** The shareable form: "st:eth:0x" + K (33 bytes) + V (33 bytes), both compressed, in hex. */
    public String encode() {
        return PREFIX
                + Hex.toHexString(spendingPublicKey.getEncoded(true))
                + Hex.toHexString(viewingPublicKey.getEncoded(true));
    }

    /**
     * Reads the text form back into K and V.
     * Throws IllegalArgumentException on a wrong prefix, wrong length, non-hex text or an invalid curve point.
     */
    public static MetaAddress parse(String metaAddress) {
        if (!metaAddress.startsWith(PREFIX)) {
            throw new IllegalArgumentException("MetaAddress should be starting with st:eth:0x");
        }

        String keysHex = metaAddress.substring(PREFIX.length());
        if (keysHex.length() != 2 * KEY_LENGTH * 2) {
            throw new IllegalArgumentException("MetaAddress should be ending with 132 hex characters");
        }

        byte[] keys;
        try {
            keys = Hex.decode(keysHex);
        } catch (DecoderException e) {
            throw new IllegalArgumentException("MetaAddress contains characters that are not hex", e);
        }

        byte[] spendingKeyBytes = Arrays.copyOfRange(keys, 0, KEY_LENGTH);
        byte[] viewingKeyBytes = Arrays.copyOfRange(keys, KEY_LENGTH, 2 * KEY_LENGTH);

        return new MetaAddress(
                Secp256k1.decodePoint(spendingKeyBytes),
                Secp256k1.decodePoint(viewingKeyBytes)
        );
    }
}
