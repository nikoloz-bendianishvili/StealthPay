package com.example.stealthpay.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.web3j.utils.Numeric;

import java.math.BigInteger;

/**
 * The spending private key k. Only the sweep reads it.
 * It may be empty: the app can scan without k. The sweep fails with a clear message only if k is actually needed.
 */
@ConfigurationProperties(prefix = "stealthpay.sweep")
public record SweepProperties(
        String spendingPrivateKey
) {

    /** k as a number, parsed from hex ("0x" prefix optional). */
    public BigInteger spendingKey() {
        if (spendingPrivateKey == null || spendingPrivateKey.isBlank()) {
            throw new IllegalStateException("stealthpay.sweep.spending-private-key is not set; it is needed to sweep");
        }
        return Numeric.toBigInt(spendingPrivateKey);
    }

    /** Hides the spending key so it can't reach the logs. */
    @Override
    public String toString() {
        return "SweepProperties[spendingPrivateKey=***]";
    }
}
