package com.example.stealthpay.chain;

import com.example.stealthpay.crypto.Announcement;
import com.example.stealthpay.crypto.Keccak;
import com.example.stealthpay.crypto.MetaAddress;
import com.example.stealthpay.crypto.Secp256k1;
import com.example.stealthpay.crypto.StealthAddressGenerator;
import org.junit.jupiter.api.Test;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Only the pure encoding is tested here; sending needs a node (see the end-to-end run on Sepolia). */
class StealthPaymentSenderTest {

    private static final MetaAddress META_ADDRESS =
            new MetaAddress(Secp256k1.multiplyG(BigInteger.TWO), Secp256k1.multiplyG(BigInteger.valueOf(3)));
    private static final BigInteger AMOUNT = new BigInteger("1000000000000000");

    @Test
    void metadataIs57BytesInTheErc5564Layout() {
        byte[] metadata = StealthPaymentSender.buildMetadata(0xAB, AMOUNT);

        assertEquals(57, metadata.length);
        assertEquals((byte) 0xAB, metadata[0]);
        assertArrayEquals(Numeric.hexStringToByteArray("0xeeeeeeee"), Arrays.copyOfRange(metadata, 1, 5));
        assertArrayEquals(Numeric.hexStringToByteArray("0xEeeeeEeeeEeEeeEeEeEeeEEEeeeeEeeeeeeeEEeE"),
                Arrays.copyOfRange(metadata, 5, 25));
        assertEquals(AMOUNT, new BigInteger(1, Arrays.copyOfRange(metadata, 25, 57)));
    }

    @Test
    void announceCallStartsWithTheAnnounceSelector() {
        Announcement announcement = StealthAddressGenerator.generate(META_ADDRESS);

        String data = StealthPaymentSender.encodeAnnounceCall(announcement, AMOUNT);

        byte[] hash = Keccak.hash("announce(uint256,address,bytes,bytes)".getBytes(StandardCharsets.UTF_8));
        String selector = Numeric.toHexString(Arrays.copyOfRange(hash, 0, 4));
        assertEquals(selector, data.substring(0, 10));
    }

    @Test
    void announceCallParametersDecodeBackToTheAnnouncement() {
        Announcement announcement = StealthAddressGenerator.generate(META_ADDRESS);

        String data = StealthPaymentSender.encodeAnnounceCall(announcement, AMOUNT);
        List<Type> params = FunctionReturnDecoder.decode(data.substring(10), announceParameterTypes());

        assertEquals(BigInteger.ONE, ((Uint256) params.get(0)).getValue());
        assertTrue(announcement.stealthAddress().equalsIgnoreCase(((Address) params.get(1)).getValue()));
        assertArrayEquals(announcement.ephemeralPublicKey().getEncoded(true), ((DynamicBytes) params.get(2)).getValue());
        assertArrayEquals(StealthPaymentSender.buildMetadata(announcement.viewTag(), AMOUNT),
                ((DynamicBytes) params.get(3)).getValue());
    }

    /** The parameter types of announce(uint256 schemeId, address stealthAddress, bytes ephemeralPubKey, bytes metadata). */
    private static List<TypeReference<Type>> announceParameterTypes() {
        return new Function("announce", List.of(), List.of(
                new TypeReference<Uint256>() {},
                new TypeReference<Address>() {},
                new TypeReference<DynamicBytes>() {},
                new TypeReference<DynamicBytes>() {}
        )).getOutputParameters();
    }
}
