package com.example.stealthpay.chain;

import com.example.stealthpay.crypto.Keccak;
import com.example.stealthpay.crypto.Secp256k1;
import org.bouncycastle.math.ec.ECPoint;
import org.junit.jupiter.api.Test;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AnnouncementReaderTest {

    private static final String STEALTH_ADDRESS = "0x7e5f4552091a69125d5dfcb7b8c2659029395bdf";
    private static final String CALLER = "0x2b5ad5c4795c026514f8317c7a215e218dccd6cf";
    private static final String TX_HASH = "0x" + "ab".repeat(32);
    private static final ECPoint R = Secp256k1.multiplyG(BigInteger.valueOf(7));

    @Test
    void announcementTopicIsKeccakOfEventSignature() {
        byte[] hash = Keccak.hash("Announcement(uint256,address,address,bytes,bytes)".getBytes(StandardCharsets.UTF_8));
        assertEquals(Numeric.toHexString(hash), AnnouncementReader.ANNOUNCEMENT_TOPIC);
    }

    @Test
    void schemeIdTopicIsOnePaddedTo32Bytes() {
        assertEquals("0x" + "0".repeat(63) + "1", AnnouncementReader.SCHEME_ID_TOPIC);
    }

    @Test
    void decodesAllFields() {
        // 0xAB = 171 is above 127, so this also checks the view tag is read as unsigned.
        byte[] metadata = StealthPaymentSender.buildMetadata(0xAB, BigInteger.valueOf(1_000_000));
        Log log = announcementLog(R.getEncoded(true), metadata);

        AnnouncementLog decoded = AnnouncementReader.decode(log).orElseThrow();

        assertEquals(STEALTH_ADDRESS, decoded.stealthAddress());
        assertEquals(R, decoded.ephemeralPublicKey());
        assertEquals(0xAB, decoded.viewTag());
        assertEquals(TX_HASH, decoded.txHash());
        assertEquals(6_000_000L, decoded.blockNumber());
        assertEquals(5, decoded.logIndex());
    }

    @Test
    void toAnnouncementKeepsAddressKeyAndViewTag() {
        Log log = announcementLog(R.getEncoded(true), new byte[]{0x12});

        AnnouncementLog decoded = AnnouncementReader.decode(log).orElseThrow();

        assertEquals(STEALTH_ADDRESS, decoded.toAnnouncement().stealthAddress());
        assertEquals(R, decoded.toAnnouncement().ephemeralPublicKey());
        assertEquals(0x12, decoded.toAnnouncement().viewTag());
    }

    @Test
    void skipsLogWithEmptyMetadata() {
        Log log = announcementLog(R.getEncoded(true), new byte[0]);

        assertEquals(Optional.empty(), AnnouncementReader.decode(log));
    }

    @Test
    void skipsLogWithUncompressedEphemeralKey() {
        Log log = announcementLog(R.getEncoded(false), new byte[]{0x12});

        assertEquals(Optional.empty(), AnnouncementReader.decode(log));
    }

    @Test
    void skipsLogWithGarbageData() {
        Log log = announcementLog(R.getEncoded(true), new byte[]{0x12});
        log.setData("0x1234");

        assertEquals(Optional.empty(), AnnouncementReader.decode(log));
    }

    @Test
    void skipsLogWithWrongEventTopic() {
        Log log = announcementLog(R.getEncoded(true), new byte[]{0x12});
        log.setTopics(List.of("0x" + "00".repeat(32), log.getTopics().get(1), log.getTopics().get(2), log.getTopics().get(3)));

        assertEquals(Optional.empty(), AnnouncementReader.decode(log));
    }

    /** Builds the log the Announcer would emit, the same way a node returns it from eth_getLogs. */
    private static Log announcementLog(byte[] ephemeralPublicKey, byte[] metadata) {
        Log log = new Log();
        log.setAddress(StealthPaymentSender.ANNOUNCER_ADDRESS);
        log.setTopics(List.of(
                AnnouncementReader.ANNOUNCEMENT_TOPIC,
                AnnouncementReader.SCHEME_ID_TOPIC,
                addressTopic(STEALTH_ADDRESS),
                addressTopic(CALLER)));
        log.setData("0x" + FunctionEncoder.encodeConstructor(
                List.of(new DynamicBytes(ephemeralPublicKey), new DynamicBytes(metadata))));
        log.setTransactionHash(TX_HASH);
        log.setBlockNumber("0x5b8d80"); // 6,000,000
        log.setLogIndex("0x5");
        return log;
    }

    private static String addressTopic(String address) {
        return "0x" + "0".repeat(24) + Numeric.cleanHexPrefix(address);
    }
}
