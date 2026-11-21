package com.example.stealthpay.chain;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthEstimateGas;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;

import java.io.IOException;
import java.math.BigInteger;

/** Signs, sends and waits for Sepolia transactions. Shared by the sender and the sweeper. */
@Component
@RequiredArgsConstructor
public class TransactionSender {

    static final long SEPOLIA_CHAIN_ID = 11155111L;

    private final Web3j web3j;

    /** Safety guard: refuses to continue if the RPC node is on any chain other than Sepolia. */
    public void requireSepolia() throws IOException {
        long chainId = web3j.ethChainId().send().getChainId().longValueExact();
        if (chainId != SEPOLIA_CHAIN_ID) {
            throw new IllegalStateException("Connected to chain " + chainId + ", but only Sepolia is allowed");
        }
    }

    /** maxFee = 2 × current base fee + tip, so the tx still fits if the base fee rises over the next blocks. */
    public GasFees currentFees() throws IOException {
        BigInteger maxPriorityFeePerGas = web3j.ethMaxPriorityFeePerGas().send().getMaxPriorityFeePerGas();
        BigInteger baseFeePerGas = web3j.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false)
                .send().getBlock().getBaseFeePerGas();
        BigInteger maxFeePerGas = baseFeePerGas.multiply(BigInteger.TWO).add(maxPriorityFeePerGas);
        return new GasFees(maxPriorityFeePerGas, maxFeePerGas);
    }

    /** Gas for a contract call: the node's estimate plus 20%, as a safety margin. */
    public BigInteger estimateGas(String from, String to, String data) throws IOException {
        return estimateWithMargin(Transaction.createFunctionCallTransaction(from, null, null, null, to, data));
    }

    /**
     * Gas for a plain ETH transfer. Don't hard-code 21,000: the gas schedule changes with network upgrades,
     * and sending ETH to a NEW account (always the case for a stealth address) costs far more than to an
     * existing one (on Sepolia: ~207k vs ~12k gas). We estimate with a value of 1 wei, because any value > 0
     * triggers the new-account cost, and 1 wei is always covered by the sender's balance.
     */
    public BigInteger estimateTransferGas(String from, String to) throws IOException {
        return estimateWithMargin(Transaction.createEtherTransaction(from, null, null, null, to, BigInteger.ONE));
    }

    /** Asks the node for the gas needed and adds 20%. Throws if the node can't estimate it. */
    private BigInteger estimateWithMargin(Transaction transaction) throws IOException {
        EthEstimateGas response = web3j.ethEstimateGas(transaction).send();
        if (response.hasError()) {
            throw new IllegalStateException("eth_estimateGas failed: " + response.getError().getMessage());
        }
        return response.getAmountUsed().multiply(BigInteger.valueOf(12)).divide(BigInteger.TEN);
    }

    /** Sends a transaction and blocks until it is mined. Returns its hash. */
    public String sendAndWait(Credentials credentials, String to, String data, BigInteger valueWei,
                              BigInteger gasLimit, GasFees fees) throws IOException, TransactionException {
        RawTransactionManager transactionManager = new RawTransactionManager(web3j, credentials, SEPOLIA_CHAIN_ID);

        EthSendTransaction sent = transactionManager.sendEIP1559Transaction(
                SEPOLIA_CHAIN_ID, fees.maxPriorityFeePerGas(), fees.maxFeePerGas(), gasLimit, to, data, valueWei);
        if (sent.hasError()) {
            throw new IllegalStateException("Transaction to " + to + " was rejected: " + sent.getError().getMessage());
        }

        TransactionReceipt receipt = new PollingTransactionReceiptProcessor(web3j, 2_000, 60)
                .waitForTransactionReceipt(sent.getTransactionHash());
        if (!receipt.isStatusOK()) {
            throw new IllegalStateException("Transaction " + sent.getTransactionHash() + " failed on chain");
        }
        return sent.getTransactionHash();
    }
}
