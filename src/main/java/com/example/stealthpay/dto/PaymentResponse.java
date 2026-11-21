package com.example.stealthpay.dto;

import com.example.stealthpay.persistence.PaymentStatus;
import com.example.stealthpay.persistence.StealthPayment;

import java.math.BigInteger;

public record PaymentResponse(
        String stealthAddress,
        BigInteger amountWei,
        String txHash,
        long blockNumber,
        PaymentStatus status
) {

    /** Copies the public fields of a saved payment. R and the view tag are left out. */
    public static PaymentResponse from(StealthPayment stealthPayment) {
        return new PaymentResponse(stealthPayment.getStealthAddress(), stealthPayment.getAmountWei(),
                                    stealthPayment.getTxHash(), stealthPayment.getBlockNumber(),
                                    stealthPayment.getStatus());
    }
}
