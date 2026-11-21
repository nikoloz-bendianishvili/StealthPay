package com.example.stealthpay.config;

import com.example.stealthpay.crypto.Secp256k1;
import org.bouncycastle.math.ec.ECPoint;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.web3j.utils.Numeric;

import java.math.BigInteger;

/**
 * Settings for the scanner. The keys arrive as hex strings from env vars and are parsed on demand:
 * v is a number (a private key), K is a curve point (a public key).
 */
@ConfigurationProperties(prefix = "stealthpay.scanner")
public record ScannerProperties(
        String viewingPrivateKey,
        String spendingPublicKey,
        long startBlock,
        int confirmations,
        int maxBlocksPerBatch
) {

    /** Checks the settings at startup, so bad config stops the app right away. */
    public ScannerProperties {
        if (viewingPrivateKey == null || viewingPrivateKey.isBlank()) {
            throw new IllegalArgumentException("stealthpay.scanner.viewing-private-key is missing");
        }
        if (spendingPublicKey == null || spendingPublicKey.isBlank()) {
            throw new IllegalArgumentException("stealthpay.scanner.spending-public-key is missing");
        }
        if (startBlock < 0) {
            throw new IllegalArgumentException("stealthpay.scanner.start-block must be >= 0");
        }
        if (confirmations < 0) {
            throw new IllegalArgumentException("stealthpay.scanner.confirmations must be >= 0");
        }
        if (maxBlocksPerBatch <= 0) {
            throw new IllegalArgumentException("stealthpay.scanner.max-blocks-per-batch must be > 0");
        }
    }

    /** v as a number, parsed from hex ("0x" prefix optional). */
    public BigInteger viewingKey() {
        return Numeric.toBigInt(viewingPrivateKey);
    }

    /** K as a curve point, parsed from 33-byte compressed hex. */
    public ECPoint spendingKey() {
        return Secp256k1.decodePoint(Numeric.hexStringToByteArray(spendingPublicKey));
    }

    /** Hides the viewing key so it can't reach the logs. */
    @Override
    public String toString() {
        return "ScannerProperties[viewingPrivateKey=***, spendingPublicKey=" + spendingPublicKey
                + ", startBlock=" + startBlock + ", confirmations=" + confirmations
                + ", maxBlocksPerBatch=" + maxBlocksPerBatch + "]";
    }
}
