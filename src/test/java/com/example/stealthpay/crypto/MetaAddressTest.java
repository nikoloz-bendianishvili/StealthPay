package com.example.stealthpay.crypto;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MetaAddressTest {

    private static final String G_COMPRESSED =
            "0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798";
    private static final String TWO_G_COMPRESSED =
            "02c6047f9441ed7d6d3045406e95c07cd85c778e4b8cef3ca7abac09b95c709ee5";

    private static final BigInteger SDK_SPENDING_PRIVATE_KEY =
            new BigInteger("363721eb9e981558c748b824cb32a840da2b3e8957c2fc3bcb8d9c86cb87456", 16);
    private static final BigInteger SDK_VIEWING_PRIVATE_KEY =
            new BigInteger("b52a0555f6a8663d89f00365893b1ef9e38eaf2e8bc48a63319c9ea5cb4a27c5", 16);
    private static final String SDK_META_ADDRESS = "st:eth:0x"
            + "033404e82cd2a92321d51e13064ec13a0fb0192a9fdaaca1cfb47b37bd27ec1397"
            + "0390ad5eca026c05ab5cf4d620a2ac65241b11df004ddca360e954db1b26e3846e";

    @Test
    void encodeMatchesReferenceSdkMetaAddress() {
        MetaAddress metaAddress = new MetaAddress(
                Secp256k1.multiplyG(SDK_SPENDING_PRIVATE_KEY),
                Secp256k1.multiplyG(SDK_VIEWING_PRIVATE_KEY)
        );

        assertEquals(SDK_META_ADDRESS, metaAddress.encode());
    }

    @Test
    void parseMatchesReferenceSdkKeys() {
        MetaAddress metaAddress = MetaAddress.parse(SDK_META_ADDRESS);

        assertEquals(Secp256k1.multiplyG(SDK_SPENDING_PRIVATE_KEY), metaAddress.spendingPublicKey());
        assertEquals(Secp256k1.multiplyG(SDK_VIEWING_PRIVATE_KEY), metaAddress.viewingPublicKey());
    }

    @Test
    void encodeGivesKnownValue() {
        MetaAddress metaAddress = new MetaAddress(
                Secp256k1.multiplyG(BigInteger.ONE),
                Secp256k1.multiplyG(BigInteger.TWO)
        );

        assertEquals("st:eth:0x" + G_COMPRESSED + TWO_G_COMPRESSED, metaAddress.encode());
    }

    @Test
    void encodedMetaAddressHasPrefixAnd132HexCharacters() {
        MetaAddress metaAddress = new MetaAddress(
                Secp256k1.multiplyG(Secp256k1.generatePrivateKey()),
                Secp256k1.multiplyG(Secp256k1.generatePrivateKey())
        );

        String encoded = metaAddress.encode();

        assertEquals("st:eth:0x", encoded.substring(0, 9));
        assertEquals(9 + 132, encoded.length());
    }

    @Test
    void parseGivesKnownKeys() {
        MetaAddress metaAddress = MetaAddress.parse("st:eth:0x" + G_COMPRESSED + TWO_G_COMPRESSED);

        assertEquals(Secp256k1.multiplyG(BigInteger.ONE), metaAddress.spendingPublicKey());
        assertEquals(Secp256k1.multiplyG(BigInteger.TWO), metaAddress.viewingPublicKey());
    }

    @Test
    void parsingEncodedMetaAddressGivesSameKeys() {
        for (int i = 0; i < 20; i++) {
            MetaAddress original = new MetaAddress(
                    Secp256k1.multiplyG(Secp256k1.generatePrivateKey()),
                    Secp256k1.multiplyG(Secp256k1.generatePrivateKey())
            );

            MetaAddress parsed = MetaAddress.parse(original.encode());

            assertEquals(original, parsed);
        }
    }

    @Test
    void spendingKeyComesBeforeViewingKey() {
        MetaAddress metaAddress = MetaAddress.parse("st:eth:0x" + TWO_G_COMPRESSED + G_COMPRESSED);

        assertEquals(Secp256k1.multiplyG(BigInteger.TWO), metaAddress.spendingPublicKey());
        assertEquals(Secp256k1.multiplyG(BigInteger.ONE), metaAddress.viewingPublicKey());
    }

    @Test
    void wrongPrefixIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> MetaAddress.parse("st:btc:0x" + G_COMPRESSED + TWO_G_COMPRESSED));
        assertThrows(IllegalArgumentException.class,
                () -> MetaAddress.parse(G_COMPRESSED + TWO_G_COMPRESSED));
    }

    @Test
    void tooShortMetaAddressIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> MetaAddress.parse("st:eth:0x" + G_COMPRESSED));
    }

    @Test
    void tooLongMetaAddressIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> MetaAddress.parse("st:eth:0x" + G_COMPRESSED + TWO_G_COMPRESSED + "00"));
    }

    @Test
    void nonHexCharactersAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> MetaAddress.parse("st:eth:0x" + "zz".repeat(66)));
    }

    @Test
    void keyThatIsNotOnTheCurveIsRejected() {
        String notOnCurve = "02" + "00".repeat(31) + "05";

        assertThrows(IllegalArgumentException.class,
                () -> MetaAddress.parse("st:eth:0x" + G_COMPRESSED + notOnCurve));
    }

    @Test
    void keyWithWrongPrefixIsRejected() {
        String wrongPrefix = "04" + G_COMPRESSED.substring(2);

        assertThrows(IllegalArgumentException.class,
                () -> MetaAddress.parse("st:eth:0x" + wrongPrefix + TWO_G_COMPRESSED));
    }
}
