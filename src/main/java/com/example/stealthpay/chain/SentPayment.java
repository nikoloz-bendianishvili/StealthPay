package com.example.stealthpay.chain;

import com.example.stealthpay.crypto.Announcement;

public record SentPayment(
   String transferTxHash,
   String announceTxHash,
   Announcement announcement
) {}
