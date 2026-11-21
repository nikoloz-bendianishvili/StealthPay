package com.example.stealthpay.dto;

import com.example.stealthpay.chain.SentPayment;

/** The one-time address that was paid, and the two txs: the ETH transfer and the announce() call. */
public record SendResponse(
        String stealthAddress,
        String transferTxHash,
        String announceTxHash
) {

    /** Flattens a sent payment into the JSON response. */
    public static SendResponse from(SentPayment sentPayment) {
        return new SendResponse(sentPayment.announcement().stealthAddress(),
                sentPayment.transferTxHash(), sentPayment.announceTxHash());
    }
}
