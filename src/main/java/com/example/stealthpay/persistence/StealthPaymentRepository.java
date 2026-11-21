package com.example.stealthpay.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigInteger;
import java.util.List;
import java.util.UUID;

public interface StealthPaymentRepository extends JpaRepository<StealthPayment, UUID> {

    /** True if this announcement is already saved. (txHash, logIndex) identifies exactly one event. */
    boolean existsByTxHashAndLogIndex(String txHash, int logIndex);

    /** All payments with this status. The sweep uses it to find the UNSPENT ones. */
    List<StealthPayment> findByStatus(PaymentStatus status);

    /** Total wei of all payments with this status, or 0 if there are none. */
    @Query("select coalesce(sum(p.amountWei), 0) from StealthPayment p where p.status = :status")
    BigInteger sumAmountByStatus(@Param("status") PaymentStatus status);
}
