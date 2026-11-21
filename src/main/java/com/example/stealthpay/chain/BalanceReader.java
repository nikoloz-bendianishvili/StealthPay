package com.example.stealthpay.chain;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;

import java.io.IOException;
import java.math.BigInteger;

@Component
@RequiredArgsConstructor
public class BalanceReader {

    private final Web3j web3j;

    /**
     * Current balance in wei. We read "latest" instead of the balance at the announcement's block,
     * because normal (non-archive) RPC nodes only keep state for recent blocks.
     * A stealth address is used once and untouched until we sweep it, so the latest balance is the payment.
     */
    public BigInteger balance(String address) throws IOException {
        EthGetBalance response = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send();
        if (response.hasError()) {
            throw new IllegalStateException("eth_getBalance failed: " + response.getError().getMessage());
        }
        return response.getBalance();
    }
}
