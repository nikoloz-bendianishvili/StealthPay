package com.example.stealthpay.config;

import com.example.stealthpay.crypto.MetaAddress;
import com.example.stealthpay.crypto.Secp256k1;
import org.bouncycastle.math.ec.ECPoint;
import org.web3j.utils.Numeric;

import java.math.BigInteger;

/**
 * One-time setup (step 1 of the flow). Run this main() by hand, never from the app.
 * It prints new keys as .env lines and the meta-address to share with senders.
 * The private keys are printed to your console on purpose, so you can copy them. Don't save that output anywhere else.
 */
public class KeyGenerator {

    public static void main(String[] args) {
        BigInteger spendingPrivateKey = Secp256k1.generatePrivateKey();
        BigInteger viewingPrivateKey = Secp256k1.generatePrivateKey();

        ECPoint spendingPublicKey = Secp256k1.multiplyG(spendingPrivateKey);
        ECPoint viewingPublicKey = Secp256k1.multiplyG(viewingPrivateKey);
        MetaAddress metaAddress = new MetaAddress(spendingPublicKey, viewingPublicKey);

        System.out.println("# Put these lines into .env");
        System.out.println("STEALTHPAY_SPENDING_PRIVATE_KEY=" + Numeric.toHexStringWithPrefixZeroPadded(spendingPrivateKey, 64));
        System.out.println("STEALTHPAY_VIEWING_PRIVATE_KEY=" + Numeric.toHexStringWithPrefixZeroPadded(viewingPrivateKey, 64));
        System.out.println("STEALTHPAY_SPENDING_PUBLIC_KEY=" + Numeric.toHexString(spendingPublicKey.getEncoded(true)));
        System.out.println();
        System.out.println("# Your meta-address (public, give it to senders)");
        System.out.println(metaAddress.encode());
    }
}
