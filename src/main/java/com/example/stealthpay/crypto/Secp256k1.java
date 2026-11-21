package com.example.stealthpay.crypto;

import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.security.SecureRandom;

public class Secp256k1 {

    private static final X9ECParameters CURVE = CustomNamedCurves.getByName("secp256k1");
    private static final ECPoint G = CURVE.getG();
    private static final BigInteger N = CURVE.getN();

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** Public key of a private key (key·G), encoded as 33 bytes if compressed, otherwise 65 bytes. */
    public static byte[] publicKey(BigInteger privateKey, boolean compressed) {
        if (privateKey.signum() <= 0 || privateKey.compareTo(N) >= 0) {
            throw new IllegalArgumentException("Private key must be between 1 and n - 1");
        }

        ECPoint publicPoint = G.multiply(privateKey).normalize();
        return publicPoint.getEncoded(compressed);
    }

    /** Secure random key in [1, n−1]. Values outside the range are thrown away and drawn again. */
    public static BigInteger generatePrivateKey() {
        BigInteger curr = BigInteger.ZERO;
        while(curr.signum() <= 0 || curr.compareTo(N) >= 0) {
            curr = new BigInteger(256, SECURE_RANDOM);
        }
        return curr;
    }

    /**
     * Turns a 33-byte compressed key into a curve point. The prefix 02/03 says whether y is even or odd.
     * Throws IllegalArgumentException if the bytes are not a valid point on the curve.
     */
    public static ECPoint decodePoint(byte[] compressedPublicKey) {
        if (compressedPublicKey.length != 33 || (compressedPublicKey[0] != 0x02 && compressedPublicKey[0] != 0x03)) {
            throw new IllegalArgumentException("Compressed public key should have length of 33 and its first byte should be 02 or 03");
        }

        return CURVE.getCurve().decodePoint(compressedPublicKey).normalize();
    }

    /** scalar·point, used for the shared secret: S = r·V on the sender side, S = v·R on the recipient side. */
    public static ECPoint multiply(ECPoint point, BigInteger scalar) {
        if (scalar.signum() <= 0 || scalar.compareTo(N) >= 0) {
            throw new IllegalArgumentException("Scalar key must be between 1 and n - 1");
        }

        return point.multiply(scalar).normalize();
    }

    /** a + b on the curve, used for P = K + s·G. Fails if the result is the point at infinity (not a usable key). */
    public static ECPoint add(ECPoint a, ECPoint b) {
        ECPoint result = a.add(b).normalize();

        if(result.isInfinity()) {
            throw new RuntimeException("ECPoint points to infinity");
        }

        return result;
    }

    /** scalar·G: turns a private number into its public point. */
    public static ECPoint multiplyG(BigInteger scalar) {
        if (scalar.signum() <= 0 || scalar.compareTo(N) >= 0) {
            throw new IllegalArgumentException("Scalar key must be between 1 and n - 1");
        }

        return G.multiply(scalar).normalize();
    }

    /** (k + s) mod n: the stealth private key. Fails on 0, which is not a valid key. */
    public static BigInteger addModN(BigInteger k, BigInteger s) {
        BigInteger result = k.add(s).mod(N);
        if(result.equals(BigInteger.ZERO)) {
            throw new IllegalArgumentException("k + s mod n shouldn't be zero");
        }
        return result;
    }

}
