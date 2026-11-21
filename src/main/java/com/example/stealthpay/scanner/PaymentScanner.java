package com.example.stealthpay.scanner;

import com.example.stealthpay.chain.AnnouncementLog;
import com.example.stealthpay.chain.AnnouncementReader;
import com.example.stealthpay.chain.BalanceReader;
import com.example.stealthpay.config.ScannerProperties;
import com.example.stealthpay.crypto.StealthAddressChecker;
import com.example.stealthpay.persistence.ScannerState;
import com.example.stealthpay.persistence.ScannerStateRepository;
import com.example.stealthpay.persistence.StealthPayment;
import com.example.stealthpay.persistence.StealthPaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.math.ec.ECPoint;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;

/**
 * Every few seconds: read the next batch of Announcement events, keep the ones that are ours,
 * and remember how far we got.
 */
@Slf4j
@Component
public class PaymentScanner {

    private static final long SCANNER_STATE_ID = 1L;

    private final AnnouncementReader announcementReader;
    private final BalanceReader balanceReader;
    private final StealthPaymentRepository paymentRepository;
    private final ScannerStateRepository scannerStateRepository;
    private final ScannerProperties properties;

    // Parsed once at startup. A bad key stops the app right away instead of failing on every scan.
    private final BigInteger viewingPrivateKey;
    private final ECPoint spendingPublicKey;

    public PaymentScanner(AnnouncementReader announcementReader, BalanceReader balanceReader,
                          StealthPaymentRepository paymentRepository, ScannerStateRepository scannerStateRepository,
                          ScannerProperties properties) {
        this.announcementReader = announcementReader;
        this.balanceReader = balanceReader;
        this.paymentRepository = paymentRepository;
        this.scannerStateRepository = scannerStateRepository;
        this.properties = properties;
        this.viewingPrivateKey = properties.viewingKey();
        this.spendingPublicKey = properties.spendingKey();
    }

    /**
     * One batch per run. The payments and the new last block are saved in ONE transaction:
     * if anything fails, both roll back, and the next run retries the same blocks. So no block is ever skipped.
     * rollbackFor = Exception.class because by default Spring does not roll back on checked exceptions (IOException).
     */
    @Scheduled(initialDelayString = "${stealthpay.scanner.initial-delay:PT5S}",
               fixedDelayString = "${stealthpay.scanner.poll-interval:PT15S}")
    @Transactional(rollbackFor = Exception.class)
    public void scan() throws IOException {
        ScannerState state = scannerStateRepository.findById(SCANNER_STATE_ID)
                .orElseGet(() -> new ScannerState(properties.startBlock()));

        long fromBlock = state.getLastScannedBlock() + 1;
        long safeHead = announcementReader.safeHeadBlock(properties.confirmations());
        long toBlock = Math.min(safeHead, fromBlock + properties.maxBlocksPerBatch() - 1);
        if (fromBlock > toBlock) {
            return;
        }

        int found = 0;
        for (AnnouncementLog announcement : announcementReader.read(fromBlock, toBlock)) {
            if (saveIfOurs(announcement)) {
                found++;
            }
        }

        state.advanceTo(toBlock);
        scannerStateRepository.save(state);
        log.info("Scanned blocks {}..{}, found {} new payment(s)", fromBlock, toBlock, found);
    }

    /**
     * Saves the announcement as an UNSPENT payment if it is ours and not saved yet.
     * Returns true only when a new payment was saved.
     */
    private boolean saveIfOurs(AnnouncementLog announcement) throws IOException {
        if (!StealthAddressChecker.isMine(announcement.toAnnouncement(), viewingPrivateKey, spendingPublicKey)) {
            return false;
        }
        // The unique (tx_hash, log_index) constraint would catch this too; checking first avoids a failed transaction.
        if (paymentRepository.existsByTxHashAndLogIndex(announcement.txHash(), announcement.logIndex())) {
            return false;
        }

        // The real amount comes from the chain, not from the metadata: anyone can write any amount there.
        BigInteger amountWei = balanceReader.balance(announcement.stealthAddress());
        if (amountWei.signum() == 0) {
            log.warn("Payment to {} has an announcement but no ETH (yet)", announcement.stealthAddress());
        }

        paymentRepository.save(new StealthPayment(
                announcement.stealthAddress(),
                Numeric.toHexString(announcement.ephemeralPublicKey().getEncoded(true)),
                announcement.viewTag(),
                amountWei,
                announcement.txHash(),
                announcement.blockNumber(),
                announcement.logIndex()));
        log.info("Found payment of {} wei to {}", amountWei, announcement.stealthAddress());
        return true;
    }
}
