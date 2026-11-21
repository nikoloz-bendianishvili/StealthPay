package com.example.stealthpay.persistence;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "scanner_state")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScannerState {

    @Id
    private Long id = 1L;

    @Column(name = "last_block", nullable = false)
    private Long lastScannedBlock;


    /** Starts one block before startBlock, so the first scan begins exactly at startBlock. */
    public ScannerState(Long startBlock) {
        this.lastScannedBlock = startBlock - 1;
    }

    /** Records how far the scanner got. Staying on the same block is fine; moving back means a bug, so it throws. */
    public void advanceTo(Long block) {
        if (block < lastScannedBlock) {
            throw new IllegalArgumentException("Cannot move back from " + lastScannedBlock + " to " + block);
        }
        this.lastScannedBlock = block;
    }
}
