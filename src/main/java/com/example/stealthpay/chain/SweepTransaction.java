package com.example.stealthpay.chain;

import java.math.BigInteger;

/** A sweep sent on chain: its tx hash and how much reached the destination. */
public record SweepTransaction(
        String txHash,
        BigInteger amountWei
) {}
