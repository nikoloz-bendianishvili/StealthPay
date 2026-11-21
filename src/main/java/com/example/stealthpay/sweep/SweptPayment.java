package com.example.stealthpay.sweep;

import java.math.BigInteger;

/** One stealth address that was emptied into the destination. */
public record SweptPayment(
        String stealthAddress,
        String txHash,
        BigInteger amountWei
) {}
