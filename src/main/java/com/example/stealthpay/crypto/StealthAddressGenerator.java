package com.example.stealthpay.crypto;

import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;


public class StealthAddressGenerator {

    /** Sender side: creates a new one-time address for this meta-address, using a fresh random r. */
    public static Announcement generate(MetaAddress metaAddress) {
        return generate(metaAddress, Secp256k1.generatePrivateKey());
    }

    /**
     * The math with a given r: R = r·G, S = r·V, s = keccak256(S), P = K + s·G, address = address of P.
     * r must never be reused: the same r and meta-address always give the same stealth address.
     */
    static Announcement generate(MetaAddress metaAddress, BigInteger r) {
        ECPoint R = Secp256k1.multiplyG(r);
        ECPoint S = Secp256k1.multiply(metaAddress.viewingPublicKey(), r);
        byte[] s = SharedSecret.hash(S);
        int viewTag = SharedSecret.viewTag(s);
        ECPoint P = Secp256k1.add(metaAddress.spendingPublicKey(), Secp256k1.multiplyG(new BigInteger(1, s)));
        String stealthAddress = EthereumAddress.fromPublicKey(P.getEncoded(false));
        return new Announcement(stealthAddress, R, viewTag);
    }


}
