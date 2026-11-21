package com.example.stealthpay.api;

import com.example.stealthpay.config.ScannerProperties;
import com.example.stealthpay.crypto.MetaAddress;
import com.example.stealthpay.crypto.Secp256k1;
import com.example.stealthpay.dto.MetaAddressResponse;
import lombok.RequiredArgsConstructor;
import org.bouncycastle.math.ec.ECPoint;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class MetaAddressController {

    private final ScannerProperties scannerProperties;

    /** The address to give to senders. Computed from K and v on each call, never stored. */
    @GetMapping("/meta-address")
    public MetaAddressResponse getMetaAddress() {
        ECPoint K = scannerProperties.spendingKey();
        ECPoint V = Secp256k1.multiplyG(scannerProperties.viewingKey());
        return new MetaAddressResponse(new MetaAddress(K, V).encode());
    }
}
