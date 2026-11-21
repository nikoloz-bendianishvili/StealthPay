package com.example.stealthpay.chain;

import com.example.stealthpay.config.ChainProperties;
import com.example.stealthpay.crypto.Announcement;
import com.example.stealthpay.crypto.MetaAddress;
import com.example.stealthpay.crypto.StealthAddressGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StealthPaymentSender {

    static final String ANNOUNCER_ADDRESS = "0x55649E01B5Df198D18D95b5cc5051630cfD45564";
    static final BigInteger SCHEME_ID = BigInteger.ONE;

    private static final byte[] ETH_TOKEN_MARKER = Numeric.hexStringToByteArray("0xeeeeeeee");
    private static final byte[] ETH_TOKEN_ADDRESS =
            Numeric.hexStringToByteArray("0xEeeeeEeeeEeEeeEeEeEeeEEEeeeeEeeeeeeeEEeE");

    private final ChainProperties chainProperties;
    private final TransactionSender transactionSender;

    /**
     * Pays a meta-address in two txs: ETH to a new stealth address, then announce() so the recipient can find it.
     * Blocks until both are mined. If the announce fails after the transfer, R is lost and the ETH is unreachable.
     */
    public SentPayment send(MetaAddress metaAddress, BigInteger amountWei) throws IOException, TransactionException {
        transactionSender.requireSepolia();

        Announcement announcement = StealthAddressGenerator.generate(metaAddress);
        Credentials credentials = Credentials.create(chainProperties.senderPrivateKey());

        BigInteger transferGasLimit = transactionSender.estimateTransferGas(
                credentials.getAddress(), announcement.stealthAddress());
        String transferTxHash = transactionSender.sendAndWait(
                credentials, announcement.stealthAddress(), "", amountWei,
                transferGasLimit, transactionSender.currentFees());

        String announceData = encodeAnnounceCall(announcement, amountWei);
        BigInteger announceGasLimit = transactionSender.estimateGas(credentials.getAddress(), ANNOUNCER_ADDRESS, announceData);
        String announceTxHash = transactionSender.sendAndWait(
                credentials, ANNOUNCER_ADDRESS, announceData, BigInteger.ZERO,
                announceGasLimit, transactionSender.currentFees());

        return new SentPayment(transferTxHash, announceTxHash, announcement);
    }

    /** ABI-encoded call data for announce(schemeId, stealthAddress, R, metadata). */
    static String encodeAnnounceCall(Announcement announcement, BigInteger amountWei) {
        Function announce = new Function(
                "announce",
                List.of(
                        new Uint256(SCHEME_ID),
                        new Address(announcement.stealthAddress()),
                        new DynamicBytes(announcement.ephemeralPublicKey().getEncoded(true)),
                        new DynamicBytes(buildMetadata(announcement.viewTag(), amountWei))
                ),
                List.of()
        );
        return FunctionEncoder.encode(announce);
    }

    /** 57 bytes: view tag (1) + native ETH marker (4) + ETH pseudo-token address (20) + amount (32). */
    static byte[] buildMetadata(int viewTag, BigInteger amountWei) {
        return ByteBuffer.allocate(1 + 4 + 20 + 32)
                .put((byte) viewTag)
                .put(ETH_TOKEN_MARKER)
                .put(ETH_TOKEN_ADDRESS)
                .put(Numeric.toBytesPadded(amountWei, 32))
                .array();
    }
}
