package com.example.stealthpay.chain;

import java.math.BigInteger;

/** EIP-1559 fees, both in wei per unit of gas. */
public record GasFees(
        BigInteger maxPriorityFeePerGas,
        BigInteger maxFeePerGas
) {}
