package com.example.stealthpay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Body of POST /api/payments/send. amountWei is a string so big values survive JSON. */
public record SendRequest(
        @NotBlank
        @Pattern(regexp = "^st:eth:0x[0-9a-fA-F]{132}$",
                 message = "must be st:eth:0x + 132 hex characters (K and V, 33 bytes each)")
        String metaAddress,

        @NotBlank
        @Pattern(regexp = "^[1-9][0-9]{0,77}$",
                 message = "must be a positive whole number of wei, at most 78 digits")
        String amountWei
) {}
