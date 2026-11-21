package com.example.stealthpay.api;

import com.example.stealthpay.dto.Balance;
import com.example.stealthpay.dto.PaymentResponse;
import com.example.stealthpay.persistence.PaymentStatus;
import com.example.stealthpay.persistence.StealthPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PaymentController {

    private final StealthPaymentRepository stealthPaymentRepository;

    /** Payments found by the scanner: all of them, or only those with the given status. */
    @GetMapping("/payments")
    public List<PaymentResponse> getPayments(@RequestParam(required = false) PaymentStatus status) {
        if(status == null) {
            return stealthPaymentRepository.findAll().stream().map(PaymentResponse::from).toList();
        } else {
            return stealthPaymentRepository.findByStatus(status).stream().map(PaymentResponse::from).toList();
        }
    }

    /** Spendable total: the sum of all UNSPENT payments, from the database (no chain call). */
    @GetMapping("/balance")
    public Balance getBalance() {
        return new Balance(stealthPaymentRepository.sumAmountByStatus(PaymentStatus.UNSPENT));
    }
}
