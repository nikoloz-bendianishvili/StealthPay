package com.example.stealthpay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Body of POST /api/sweeps. Checked by @Valid before the controller method runs; a failure gives 400. */
public record MoveMoneyRequest(
        @NotBlank
        @Pattern(regexp = "^0x[0-9a-fA-F]{40}$", message = "must be an Ethereum address: 0x + 40 hex characters")
        String destination
) {}
