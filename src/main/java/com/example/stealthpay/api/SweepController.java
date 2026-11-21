package com.example.stealthpay.api;

import com.example.stealthpay.dto.MoveMoneyRequest;
import com.example.stealthpay.dto.SweepResponse;
import com.example.stealthpay.sweep.SweepService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.web3j.protocol.exceptions.TransactionException;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SweepController {

    private final SweepService sweepService;

    /** Empties every UNSPENT stealth address into the destination. Slow: it waits for each tx to be mined. */
    @PostMapping("/sweeps")
    public List<SweepResponse> moveMyMoney(@RequestBody @Valid MoveMoneyRequest moveMoneyRequest)
            throws IOException, TransactionException {
        return sweepService.sweepAll(moveMoneyRequest.destination()).stream()
                .map(SweepResponse::from)
                .toList();
    }
}
