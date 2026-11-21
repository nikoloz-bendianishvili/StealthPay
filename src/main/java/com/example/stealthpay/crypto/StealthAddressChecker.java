package com.example.stealthpay.crypto;

import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;


public class StealthAddressChecker {

    /**
     * Recipient side: is this announcement a payment to us? Needs only v and K, so it can never spend.
     * The view tag is checked first because it is cheap; only matches go on to the full address comparison.
     */
    public static boolean isMine(Announcement announcement, BigInteger viewingPrivateKey, ECPoint spendingPublicKey) {
        byte[] s = hashedSharedSecret(announcement.ephemeralPublicKey(), viewingPrivateKey);

        if(SharedSecret.viewTag(s) != announcement.viewTag()) {
            return false;
        }

        ECPoint P = Secp256k1.add(spendingPublicKey, Secp256k1.multiplyG(new BigInteger(1, s)));
        String stealthAddress = EthereumAddress.fromPublicKey(P.getEncoded(false));
        return announcement.stealthAddress().equalsIgnoreCase(stealthAddress);
    }

    /** Private key that controls a stealth address: (k + s) mod n. Needed to sign the sweep. */
    public static BigInteger stealthPrivateKey(ECPoint ephemeralPublicKey, BigInteger viewingPrivateKey,
                                               BigInteger spendingPrivateKey) {
        byte[] s = hashedSharedSecret(ephemeralPublicKey, viewingPrivateKey);
        return Secp256k1.addModN(spendingPrivateKey, new BigInteger(1, s));
    }

    /** s = keccak256(v·R), the same value the sender got from keccak256(r·V). */
    private static byte[] hashedSharedSecret(ECPoint ephemeralPublicKey, BigInteger viewingPrivateKey) {
        ECPoint S = Secp256k1.multiply(ephemeralPublicKey, viewingPrivateKey);
        return SharedSecret.hash(S);
    }
}
