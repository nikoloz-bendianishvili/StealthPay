package com.example.stealthpay.persistence;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.util.UUID;

@Entity
@Table(
        name = "stealth_payment",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tx_hash", "log_index"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StealthPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** One-time address the ETH was sent to, "0x" + 40 hex chars. */
    @Column(name = "stealth_address", nullable = false, length = 42)
    private String stealthAddress;

    /** R = r·G, compressed (33 bytes) as "0x" + 66 hex chars. Needed to derive the stealth private key again. */
    @Column(name = "ephemeral_public_key", nullable = false, length = 68)
    private String ephemeralPublicKey;

    /** First byte of s, 0..255. */
    @Column(name = "view_tag", nullable = false)
    private int viewTag;

    /** Amount in wei. */
    @Column(name = "amount_wei", nullable = false, precision = 78, scale = 0)
    private BigInteger amountWei;

    @Column(name = "tx_hash", nullable = false, length = 66)
    private String txHash;

    @Column(name = "block_number", nullable = false)
    private long blockNumber;

    /** Position of the Announcement event inside its block. */
    @Column(name = "log_index", nullable = false)
    private int logIndex;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private PaymentStatus status;

    /** A newly found payment always starts as UNSPENT. */
    public StealthPayment(String stealthAddress, String ephemeralPublicKey, int viewTag,
                          BigInteger amountWei, String txHash, long blockNumber, int logIndex) {
        this.stealthAddress = stealthAddress;
        this.ephemeralPublicKey = ephemeralPublicKey;
        this.viewTag = viewTag;
        this.amountWei = amountWei;
        this.txHash = txHash;
        this.blockNumber = blockNumber;
        this.logIndex = logIndex;
        this.status = PaymentStatus.UNSPENT;
    }

    /** Call after the sweep tx is mined. A SWEPT payment no longer counts toward the balance or later sweeps. */
    public void markSwept() {
        this.status = PaymentStatus.SWEPT;
    }
}
