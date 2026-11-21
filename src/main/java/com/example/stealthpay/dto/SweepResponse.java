package com.example.stealthpay.dto;

import com.example.stealthpay.sweep.SweptPayment;

/**
 * One swept stealth address. txHash is the SWEEP transaction (stealth address → destination),
 * not the original payment. amountWei is what reached the destination, after gas.
 */
public record SweepResponse(
        String stealthAddress,
        String txHash,
        String amountWei
) {

    /** Converts a swept payment. The amount becomes a string so big values survive JSON. */
    public static SweepResponse from(SweptPayment sweptPayment) {
        return new SweepResponse(sweptPayment.stealthAddress(), sweptPayment.txHash(),
                sweptPayment.amountWei().toString());
    }
}
