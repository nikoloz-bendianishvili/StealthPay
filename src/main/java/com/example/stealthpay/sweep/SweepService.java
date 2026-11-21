package com.example.stealthpay.sweep;

import com.example.stealthpay.chain.StealthAddressSweeper;
import com.example.stealthpay.chain.SweepTransaction;
import com.example.stealthpay.config.ScannerProperties;
import com.example.stealthpay.config.SweepProperties;
import com.example.stealthpay.crypto.EthereumAddress;
import com.example.stealthpay.crypto.Secp256k1;
import com.example.stealthpay.crypto.StealthAddressChecker;
import com.example.stealthpay.persistence.PaymentStatus;
import com.example.stealthpay.persistence.StealthPayment;
import com.example.stealthpay.persistence.StealthPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.math.ec.ECPoint;
import org.springframework.stereotype.Service;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Empties every UNSPENT stealth address into one destination.
 * Known privacy limit: sweeping many stealth addresses into one address links them on chain.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SweepService {

    private final StealthPaymentRepository paymentRepository;
    private final StealthAddressSweeper sweeper;
    private final ScannerProperties scannerProperties;
    private final SweepProperties sweepProperties;

    /**
     * Deliberately NOT one big @Transactional: every on-chain sweep is final, so each payment is marked SWEPT
     * right after its own tx. If payment 3 of 5 fails, payments 1 and 2 stay correctly marked.
     */
    public List<SweptPayment> sweepAll(String destination) throws IOException, TransactionException {
        if (!WalletUtils.isValidAddress(destination)) {
            throw new IllegalArgumentException("Not a valid Ethereum address: " + destination);
        }

        List<StealthPayment> unspent = paymentRepository.findByStatus(PaymentStatus.UNSPENT);
        if (unspent.isEmpty()) {
            return List.of();
        }

        BigInteger spendingPrivateKey = sweepProperties.spendingKey();
        BigInteger viewingPrivateKey = scannerProperties.viewingKey();

        List<SweptPayment> swept = new ArrayList<>();
        for (StealthPayment payment : unspent) {
            BigInteger stealthPrivateKey = stealthPrivateKey(payment, viewingPrivateKey, spendingPrivateKey);

            Optional<SweepTransaction> transaction = sweeper.sweep(stealthPrivateKey, destination);
            if (transaction.isEmpty()) {
                log.warn("Skipped {}: its balance does not cover the gas", payment.getStealthAddress());
                continue;
            }

            payment.markSwept();
            paymentRepository.save(payment);
            swept.add(new SweptPayment(payment.getStealthAddress(), transaction.get().txHash(), transaction.get().amountWei()));
            log.info("Swept {} wei from {} in tx {}", transaction.get().amountWei(), payment.getStealthAddress(),
                    transaction.get().txHash());
        }
        return swept;
    }

    /**
     * (k + s) mod n, derived again from the stored R. As a safety check, the key must control the stored address.
     * A mismatch means the wrong k or v is configured, and we'd better stop than send from the wrong key.
     */
    static BigInteger stealthPrivateKey(StealthPayment payment, BigInteger viewingPrivateKey, BigInteger spendingPrivateKey) {
        ECPoint ephemeralPublicKey = Secp256k1.decodePoint(Numeric.hexStringToByteArray(payment.getEphemeralPublicKey()));
        BigInteger stealthPrivateKey =
                StealthAddressChecker.stealthPrivateKey(ephemeralPublicKey, viewingPrivateKey, spendingPrivateKey);

        String derivedAddress = EthereumAddress.fromPublicKey(Secp256k1.publicKey(stealthPrivateKey, false));
        if (!derivedAddress.equalsIgnoreCase(payment.getStealthAddress())) {
            throw new IllegalStateException("Derived key does not control " + payment.getStealthAddress()
                    + ". Is the right spending key configured?");
        }
        return stealthPrivateKey;
    }
}
