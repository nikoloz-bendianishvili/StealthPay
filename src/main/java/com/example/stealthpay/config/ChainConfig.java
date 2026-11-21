package com.example.stealthpay.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

@Configuration
@EnableConfigurationProperties({ChainProperties.class, ScannerProperties.class, SweepProperties.class})
public class ChainConfig {

    /** The one client for the Sepolia node, shared by all of `chain`. Closed when the app stops. */
    @Bean(destroyMethod = "shutdown")
    public Web3j web3j(ChainProperties chainProperties) {
        return Web3j.build(new HttpService(chainProperties.rpcUrl()));
    }
}
