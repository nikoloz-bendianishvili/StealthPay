package com.example.stealthpay.chain;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StealthAddressSweeperTest {

    private static final GasFees FEES = new GasFees(BigInteger.valueOf(1_000_000_000), BigInteger.valueOf(10_000_000_000L));
    private static final BigInteger BALANCE = new BigInteger("1000000000000000"); // 0.001 ETH

    @Test
    void amountAfterGasReservesGasLimitTimesMaxFee() {
        BigInteger gasLimit = BigInteger.valueOf(21_000);

        // 21,000 gas × 10 gwei = 210,000,000,000,000 wei
        assertEquals(new BigInteger("790000000000000"), StealthAddressSweeper.amountAfterGas(BALANCE, gasLimit, FEES));
    }

    @Test
    void newAccountDestinationReservesMoreGas() {
        BigInteger gasLimit = BigInteger.valueOf(80_000);

        // 80,000 gas × 10 gwei = 800,000,000,000,000 wei
        assertEquals(new BigInteger("200000000000000"), StealthAddressSweeper.amountAfterGas(BALANCE, gasLimit, FEES));
    }

    @Test
    void amountAfterGasIsNotPositiveWhenBalanceIsTooSmall() {
        BigInteger balance = BigInteger.valueOf(1_000);

        assertTrue(StealthAddressSweeper.amountAfterGas(balance, BigInteger.valueOf(21_000), FEES).signum() <= 0);
    }
}
