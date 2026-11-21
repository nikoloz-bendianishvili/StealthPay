package com.example.stealthpay.api;

import com.example.stealthpay.chain.StealthPaymentSender;
import com.example.stealthpay.crypto.MetaAddress;
import com.example.stealthpay.dto.SendRequest;
import com.example.stealthpay.dto.SendResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.web3j.protocol.exceptions.TransactionException;

import java.io.IOException;
import java.math.BigInteger;

/** Demo sender side: pays a meta-address from the sender wallet. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SendController {

    private final StealthPaymentSender stealthPaymentSender;

    /** Slow: it waits until both the transfer and the announce tx are mined. */
    @PostMapping("/payments/send")
    public SendResponse send(@RequestBody @Valid SendRequest sendRequest) throws IOException, TransactionException {
        MetaAddress metaAddress = MetaAddress.parse(sendRequest.metaAddress());
        BigInteger amountWei = new BigInteger(sendRequest.amountWei());

        return SendResponse.from(stealthPaymentSender.send(metaAddress, amountWei));
    }
}
