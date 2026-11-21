package com.example.stealthpay.chain;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.protocol.exceptions.TransactionException;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Optional;

/** Moves all ETH from one stealth address to a destination. The stealth address pays its own gas. */
@Component
@RequiredArgsConstructor
public class StealthAddressSweeper {

    private final TransactionSender transactionSender;
    private final BalanceReader balanceReader;

    /** Returns empty when the balance can't even pay for the gas. */
    public Optional<SweepTransaction> sweep(BigInteger stealthPrivateKey, String destination)
            throws IOException, TransactionException {
        transactionSender.requireSepolia();

        Credentials credentials = Credentials.create(ECKeyPair.create(stealthPrivateKey));
        BigInteger balance = balanceReader.balance(credentials.getAddress());
        GasFees fees = transactionSender.currentFees();
        // Depends on the destination: a new account costs much more gas than an existing one.
        BigInteger gasLimit = transactionSender.estimateTransferGas(credentials.getAddress(), destination);

        BigInteger amount = amountAfterGas(balance, gasLimit, fees);
        if (amount.signum() <= 0) {
            return Optional.empty();
        }

        String txHash = transactionSender.sendAndWait(credentials, destination, "", amount, gasLimit, fees);
        return Optional.of(new SweepTransaction(txHash, amount));
    }

    /**
     * balance − gasLimit × maxFee. The node checks that the sender can pay gasLimit × maxFee, so we reserve that much.
     * The real charge is lower (gas actually used × (base fee + tip)), so a little dust stays on the stealth address.
     */
    static BigInteger amountAfterGas(BigInteger balance, BigInteger gasLimit, GasFees fees) {
        return balance.subtract(gasLimit.multiply(fees.maxFeePerGas()));
    }
}
