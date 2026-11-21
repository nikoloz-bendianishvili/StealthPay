package com.example.stealthpay.crypto;

import org.bouncycastle.math.ec.ECPoint;

public record Announcement(
        String stealthAddress,
        ECPoint ephemeralPublicKey,
        int viewTag
) {}
