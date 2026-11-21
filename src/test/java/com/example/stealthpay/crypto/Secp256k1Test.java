package com.example.stealthpay.crypto;

import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class Secp256k1Test {

    private static final BigInteger N =
            new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141", 16);

    @Test
    void privateKeyOneGivesGeneratorPoint() {
        byte[] publicKey = Secp256k1.publicKey(BigInteger.ONE, false);

        assertEquals("04"
                        + "79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798"
                        + "483ada7726a3c4655da4fbfc0e1108a8fd17b448a68554199c47d08ffb10d4b8",
                Hex.toHexString(publicKey));
    }

    @Test
    void privateKeyTwoGivesKnownPoint() {
        byte[] publicKey = Secp256k1.publicKey(BigInteger.TWO, false);

        assertEquals("04"
                        + "c6047f9441ed7d6d3045406e95c07cd85c778e4b8cef3ca7abac09b95c709ee5"
                        + "1ae168fea63dc339a3c58419466ceaeef7f632653266d0e1236431a950cfe52a",
                Hex.toHexString(publicKey));
    }

    @Test
    void uncompressedPublicKeyIs65BytesStartingWith04() {
        byte[] publicKey = Secp256k1.publicKey(new BigInteger("123456789"), false);

        assertEquals(65, publicKey.length);
        assertEquals(0x04, publicKey[0]);
    }

    @Test
    void privateKeyOneGivesCompressedGeneratorPoint() {
        byte[] publicKey = Secp256k1.publicKey(BigInteger.ONE, true);

        assertEquals("02" + "79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798",
                Hex.toHexString(publicKey));
    }

    @Test
    void privateKeyTwoGivesCompressedKnownPoint() {
        byte[] publicKey = Secp256k1.publicKey(BigInteger.TWO, true);

        assertEquals("02" + "c6047f9441ed7d6d3045406e95c07cd85c778e4b8cef3ca7abac09b95c709ee5",
                Hex.toHexString(publicKey));
    }

    @Test
    void compressedPublicKeyIs33BytesStartingWith02Or03() {
        byte[] publicKey = Secp256k1.publicKey(new BigInteger("123456789"), true);

        assertEquals(33, publicKey.length);
        assertTrue(publicKey[0] == 0x02 || publicKey[0] == 0x03);
    }

    @Test
    void compressedAndUncompressedFormsMatch() {
        for (int i = 0; i < 100; i++) {
            BigInteger privateKey = Secp256k1.generatePrivateKey();
            byte[] compressed = Secp256k1.publicKey(privateKey, true);
            byte[] uncompressed = Secp256k1.publicKey(privateKey, false);

            byte[] compressedX = Arrays.copyOfRange(compressed, 1, 33);
            byte[] uncompressedX = Arrays.copyOfRange(uncompressed, 1, 33);
            assertArrayEquals(uncompressedX, compressedX);

            boolean yIsEven = (uncompressed[64] & 1) == 0;
            byte expectedPrefix = yIsEven ? (byte) 0x02 : (byte) 0x03;
            assertEquals(expectedPrefix, compressed[0]);
        }
    }

    @Test
    void largestValidPrivateKeyIsAccepted() {
        byte[] publicKey = Secp256k1.publicKey(N.subtract(BigInteger.ONE), false);

        assertEquals(65, publicKey.length);
    }

    @Test
    void zeroPrivateKeyIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Secp256k1.publicKey(BigInteger.ZERO, false));
        assertThrows(IllegalArgumentException.class, () -> Secp256k1.publicKey(BigInteger.ZERO, true));
    }

    @Test
    void negativePrivateKeyIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Secp256k1.publicKey(BigInteger.ONE.negate(), false));
        assertThrows(IllegalArgumentException.class, () -> Secp256k1.publicKey(BigInteger.ONE.negate(), true));
    }

    @Test
    void privateKeyEqualToNIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Secp256k1.publicKey(N, false));
        assertThrows(IllegalArgumentException.class, () -> Secp256k1.publicKey(N, true));
    }

    @Test
    void generatedPrivateKeyIsAlwaysBetweenOneAndNMinusOne() {
        for (int i = 0; i < 1000; i++) {
            BigInteger privateKey = Secp256k1.generatePrivateKey();

            assertTrue(privateKey.compareTo(BigInteger.ONE) >= 0);
            assertTrue(privateKey.compareTo(N) < 0);
        }
    }

    @Test
    void twoGeneratedPrivateKeysAreDifferent() {
        BigInteger first = Secp256k1.generatePrivateKey();
        BigInteger second = Secp256k1.generatePrivateKey();

        assertNotEquals(first, second);
    }

    @Test
    void generatedPrivateKeyGivesValidPublicKey() {
        byte[] publicKey = Secp256k1.publicKey(Secp256k1.generatePrivateKey(), false);

        assertEquals(65, publicKey.length);
        assertEquals(0x04, publicKey[0]);
    }

    @Test
    void decodingCompressedKeyOfPrivateKeyOneGivesGeneratorPoint() {
        ECPoint point = Secp256k1.decodePoint(Secp256k1.publicKey(BigInteger.ONE, true));

        assertEquals("04"
                        + "79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798"
                        + "483ada7726a3c4655da4fbfc0e1108a8fd17b448a68554199c47d08ffb10d4b8",
                Hex.toHexString(point.getEncoded(false)));
    }

    @Test
    void decodingCompressedKeyGivesSamePointAsUncompressedKey() {
        for (int i = 0; i < 100; i++) {
            BigInteger privateKey = Secp256k1.generatePrivateKey();
            byte[] compressed = Secp256k1.publicKey(privateKey, true);
            byte[] uncompressed = Secp256k1.publicKey(privateKey, false);

            ECPoint point = Secp256k1.decodePoint(compressed);

            assertArrayEquals(uncompressed, point.getEncoded(false));
        }
    }

    @Test
    void compressedKeyWithWrongLengthIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Secp256k1.decodePoint(new byte[32]));
        assertThrows(IllegalArgumentException.class, () -> Secp256k1.decodePoint(new byte[34]));
    }

    @Test
    void uncompressedKeyIsRejected() {
        byte[] uncompressed = Secp256k1.publicKey(BigInteger.ONE, false);

        assertThrows(IllegalArgumentException.class, () -> Secp256k1.decodePoint(uncompressed));
    }

    @Test
    void compressedKeyWithWrongPrefixIsRejected() {
        byte[] publicKey = Secp256k1.publicKey(BigInteger.ONE, true);
        publicKey[0] = 0x04;

        assertThrows(IllegalArgumentException.class, () -> Secp256k1.decodePoint(publicKey));
    }

    @Test
    void xThatIsNotOnTheCurveIsRejected() {
        byte[] notOnCurve = new byte[33];
        notOnCurve[0] = 0x02;
        notOnCurve[32] = 5;

        assertThrows(IllegalArgumentException.class, () -> Secp256k1.decodePoint(notOnCurve));
    }

    @Test
    void multiplyingGeneratorByTwoGivesPublicKeyOfTwo() {
        ECPoint g = Secp256k1.decodePoint(Secp256k1.publicKey(BigInteger.ONE, true));

        ECPoint result = Secp256k1.multiply(g, BigInteger.TWO);

        assertArrayEquals(Secp256k1.publicKey(BigInteger.TWO, false), result.getEncoded(false));
    }

    @Test
    void multiplyingGeneratorGivesSameResultAsPublicKey() {
        ECPoint g = Secp256k1.decodePoint(Secp256k1.publicKey(BigInteger.ONE, true));

        for (int i = 0; i < 20; i++) {
            BigInteger privateKey = Secp256k1.generatePrivateKey();

            ECPoint result = Secp256k1.multiply(g, privateKey);

            assertArrayEquals(Secp256k1.publicKey(privateKey, false), result.getEncoded(false));
        }
    }

    @Test
    void senderAndRecipientGetSameSharedSecret() {
        for (int i = 0; i < 20; i++) {
            BigInteger ephemeralPrivateKey = Secp256k1.generatePrivateKey();
            BigInteger viewingPrivateKey = Secp256k1.generatePrivateKey();
            ECPoint ephemeralPublicKey = Secp256k1.decodePoint(Secp256k1.publicKey(ephemeralPrivateKey, true));
            ECPoint viewingPublicKey = Secp256k1.decodePoint(Secp256k1.publicKey(viewingPrivateKey, true));

            ECPoint senderSecret = Secp256k1.multiply(viewingPublicKey, ephemeralPrivateKey);
            ECPoint recipientSecret = Secp256k1.multiply(ephemeralPublicKey, viewingPrivateKey);

            assertArrayEquals(senderSecret.getEncoded(true), recipientSecret.getEncoded(true));
        }
    }

    @Test
    void differentScalarsGiveDifferentPoints() {
        ECPoint point = Secp256k1.decodePoint(Secp256k1.publicKey(Secp256k1.generatePrivateKey(), true));

        ECPoint first = Secp256k1.multiply(point, BigInteger.TWO);
        ECPoint second = Secp256k1.multiply(point, BigInteger.valueOf(3));

        assertFalse(Arrays.equals(first.getEncoded(true), second.getEncoded(true)));
    }

    @Test
    void largestValidScalarIsAccepted() {
        ECPoint g = Secp256k1.decodePoint(Secp256k1.publicKey(BigInteger.ONE, true));

        ECPoint result = Secp256k1.multiply(g, N.subtract(BigInteger.ONE));

        assertArrayEquals(Secp256k1.publicKey(N.subtract(BigInteger.ONE), false), result.getEncoded(false));
    }

    @Test
    void zeroScalarIsRejected() {
        ECPoint g = Secp256k1.decodePoint(Secp256k1.publicKey(BigInteger.ONE, true));

        assertThrows(IllegalArgumentException.class, () -> Secp256k1.multiply(g, BigInteger.ZERO));
    }

    @Test
    void negativeScalarIsRejected() {
        ECPoint g = Secp256k1.decodePoint(Secp256k1.publicKey(BigInteger.ONE, true));

        assertThrows(IllegalArgumentException.class, () -> Secp256k1.multiply(g, BigInteger.ONE.negate()));
    }

    @Test
    void scalarEqualToNIsRejected() {
        ECPoint g = Secp256k1.decodePoint(Secp256k1.publicKey(BigInteger.ONE, true));

        assertThrows(IllegalArgumentException.class, () -> Secp256k1.multiply(g, N));
    }

    @Test
    void multiplyGByOneGivesGeneratorPoint() {
        ECPoint result = Secp256k1.multiplyG(BigInteger.ONE);

        assertEquals("04"
                        + "79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798"
                        + "483ada7726a3c4655da4fbfc0e1108a8fd17b448a68554199c47d08ffb10d4b8",
                Hex.toHexString(result.getEncoded(false)));
    }

    @Test
    void multiplyGGivesSameResultAsPublicKey() {
        for (int i = 0; i < 20; i++) {
            BigInteger scalar = Secp256k1.generatePrivateKey();

            ECPoint result = Secp256k1.multiplyG(scalar);

            assertArrayEquals(Secp256k1.publicKey(scalar, false), result.getEncoded(false));
        }
    }

    @Test
    void multiplyGGivesSameResultAsMultiplyWithGenerator() {
        ECPoint g = Secp256k1.decodePoint(Secp256k1.publicKey(BigInteger.ONE, true));
        BigInteger scalar = Secp256k1.generatePrivateKey();

        assertArrayEquals(Secp256k1.multiply(g, scalar).getEncoded(false),
                Secp256k1.multiplyG(scalar).getEncoded(false));
    }

    @Test
    void multiplyGRejectsZeroNegativeAndN() {
        assertThrows(IllegalArgumentException.class, () -> Secp256k1.multiplyG(BigInteger.ZERO));
        assertThrows(IllegalArgumentException.class, () -> Secp256k1.multiplyG(BigInteger.ONE.negate()));
        assertThrows(IllegalArgumentException.class, () -> Secp256k1.multiplyG(N));
    }

    @Test
    void addingGeneratorToItselfGivesPublicKeyOfTwo() {
        ECPoint g = Secp256k1.multiplyG(BigInteger.ONE);

        ECPoint result = Secp256k1.add(g, g);

        assertArrayEquals(Secp256k1.publicKey(BigInteger.TWO, false), result.getEncoded(false));
    }

    @Test
    void addingTwoPublicKeysGivesPublicKeyOfSumModN() {
        for (int i = 0; i < 20; i++) {
            BigInteger a = Secp256k1.generatePrivateKey();
            BigInteger b = Secp256k1.generatePrivateKey();
            BigInteger sum = a.add(b).mod(N);

            ECPoint result = Secp256k1.add(Secp256k1.multiplyG(a), Secp256k1.multiplyG(b));

            assertArrayEquals(Secp256k1.publicKey(sum, false), result.getEncoded(false));
        }
    }

    @Test
    void addIsCommutative() {
        ECPoint a = Secp256k1.multiplyG(Secp256k1.generatePrivateKey());
        ECPoint b = Secp256k1.multiplyG(Secp256k1.generatePrivateKey());

        assertArrayEquals(Secp256k1.add(a, b).getEncoded(false), Secp256k1.add(b, a).getEncoded(false));
    }

    @Test
    void addingPointToItsNegationIsRejected() {
        BigInteger k = Secp256k1.generatePrivateKey();
        ECPoint point = Secp256k1.multiplyG(k);
        ECPoint negation = Secp256k1.multiplyG(N.subtract(k));

        assertThrows(RuntimeException.class, () -> Secp256k1.add(point, negation));
    }

    @Test
    void addModNOfOneAndTwoIsThree() {
        assertEquals(BigInteger.valueOf(3), Secp256k1.addModN(BigInteger.ONE, BigInteger.TWO));
    }

    @Test
    void addModNWrapsAroundN() {
        assertEquals(BigInteger.ONE, Secp256k1.addModN(N.subtract(BigInteger.ONE), BigInteger.TWO));
    }

    @Test
    void addModNRejectsZeroResult() {
        assertThrows(IllegalArgumentException.class,
                () -> Secp256k1.addModN(N.subtract(BigInteger.ONE), BigInteger.ONE));
    }

    @Test
    void addModNMatchesPointAddition() {
        for (int i = 0; i < 20; i++) {
            BigInteger a = Secp256k1.generatePrivateKey();
            BigInteger b = Secp256k1.generatePrivateKey();

            ECPoint fromSum = Secp256k1.multiplyG(Secp256k1.addModN(a, b));
            ECPoint fromPoints = Secp256k1.add(Secp256k1.multiplyG(a), Secp256k1.multiplyG(b));

            assertArrayEquals(fromPoints.getEncoded(false), fromSum.getEncoded(false));
        }
    }
}
