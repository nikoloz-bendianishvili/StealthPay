package com.example.stealthpay.chain;

import com.example.stealthpay.crypto.Secp256k1;
import lombok.RequiredArgsConstructor;
import org.bouncycastle.math.ec.ECPoint;
import org.springframework.stereotype.Component;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AnnouncementReader {

    /**
     * event Announcement(uint256 indexed schemeId, address indexed stealthAddress, address indexed caller,
     *                    bytes ephemeralPubKey, bytes metadata)
     * "true" marks an indexed parameter: it goes into a topic, not into the log's data.
     */
    static final Event ANNOUNCEMENT_EVENT = new Event("Announcement", List.of(
            new TypeReference<Uint256>(true) {},
            new TypeReference<Address>(true) {},
            new TypeReference<Address>(true) {},
            new TypeReference<DynamicBytes>() {},
            new TypeReference<DynamicBytes>() {}
    ));

    /** topic0 = keccak256("Announcement(uint256,address,address,bytes,bytes)"). */
    static final String ANNOUNCEMENT_TOPIC = EventEncoder.encode(ANNOUNCEMENT_EVENT);

    /** topic1 = schemeId as a 32-byte word. */
    static final String SCHEME_ID_TOPIC = Numeric.toHexStringWithPrefixZeroPadded(StealthPaymentSender.SCHEME_ID, 64);

    private final Web3j web3j;

    /** The newest block we treat as final: latest − confirmations. */
    public long safeHeadBlock(int confirmations) throws IOException {
        long latest = web3j.ethBlockNumber().send().getBlockNumber().longValueExact();
        return latest - confirmations;
    }

    /** All valid scheme-1 announcements in [fromBlock, toBlock], both ends included. */
    public List<AnnouncementLog> read(long fromBlock, long toBlock) throws IOException {
        EthFilter filter = new EthFilter(
                DefaultBlockParameter.valueOf(BigInteger.valueOf(fromBlock)),
                DefaultBlockParameter.valueOf(BigInteger.valueOf(toBlock)),
                StealthPaymentSender.ANNOUNCER_ADDRESS);
        filter.addSingleTopic(ANNOUNCEMENT_TOPIC);
        filter.addSingleTopic(SCHEME_ID_TOPIC);

        EthLog response = web3j.ethGetLogs(filter).send();
        if (response.hasError()) {
            throw new IllegalStateException("eth_getLogs failed: " + response.getError().getMessage());
        }

        List<AnnouncementLog> announcements = new ArrayList<>();
        for (EthLog.LogResult<?> result : response.getLogs()) {
            decode((Log) result.get()).ifPresent(announcements::add);
        }
        return announcements;
    }

    /**
     * Turns a raw log into an AnnouncementLog. Anyone can call announce(), so the log may be junk:
     * in that case it returns empty instead of throwing, and the scanner just moves on.
     */
    static Optional<AnnouncementLog> decode(Log log) {
        List<String> topics = log.getTopics();
        if (topics.size() != 4 || !ANNOUNCEMENT_TOPIC.equalsIgnoreCase(topics.get(0))) {
            return Optional.empty();
        }

        byte[] ephemeralPublicKeyBytes;
        byte[] metadata;
        try {
            List<Type> values = FunctionReturnDecoder.decode(log.getData(), ANNOUNCEMENT_EVENT.getNonIndexedParameters());
            ephemeralPublicKeyBytes = ((DynamicBytes) values.get(0)).getValue();
            metadata = ((DynamicBytes) values.get(1)).getValue();
        } catch (RuntimeException e) {
            return Optional.empty();
        }

        if (metadata.length == 0) {
            return Optional.empty();
        }

        ECPoint ephemeralPublicKey;
        try {
            ephemeralPublicKey = Secp256k1.decodePoint(ephemeralPublicKeyBytes);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }

        // An address topic is 32 bytes: 12 zero bytes, then the 20-byte address (the last 40 hex chars).
        String stealthAddress = "0x" + topics.get(2).substring(topics.get(2).length() - 40);
        int viewTag = metadata[0] & 0xFF;

        return Optional.of(new AnnouncementLog(
                stealthAddress,
                ephemeralPublicKey,
                viewTag,
                log.getTransactionHash(),
                log.getBlockNumber().longValueExact(),
                log.getLogIndex().intValueExact()));
    }
}
