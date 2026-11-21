package com.example.stealthpay.chain;

import com.example.stealthpay.crypto.Announcement;
import org.bouncycastle.math.ec.ECPoint;

/** One decoded Announcement event, plus where it is on the chain. */
public record AnnouncementLog(
        String stealthAddress,
        ECPoint ephemeralPublicKey,
        int viewTag,
        String txHash,
        long blockNumber,
        int logIndex
) {

    /** Keeps only what the crypto code needs, without the on-chain position. */
    public Announcement toAnnouncement() {
        return new Announcement(stealthAddress, ephemeralPublicKey, viewTag);
    }
}
