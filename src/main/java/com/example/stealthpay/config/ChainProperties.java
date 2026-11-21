package com.example.stealthpay.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stealthpay.chain")
public record ChainProperties(
        String rpcUrl,
        String senderPrivateKey
) {

    /** Hides both values: the sender key is secret, and the RPC URL contains an API key. */
    @Override
    public String toString() {
        return "ChainProperties[rpcUrl=***, senderPrivateKey=***]";
    }
}
