package com.example.stealthpay.api;

import com.example.stealthpay.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.web3j.protocol.exceptions.TransactionException;

import java.io.IOException;

/**
 * Turns exceptions from any controller into a short JSON error, never a stack trace.
 * None of our exception messages contain keys, so passing the message through is safe.
 * A failed @Valid is not handled here: Spring already answers it with 400.
 */
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    /** Bad input that passed the DTO checks but was rejected deeper, e.g. by SweepService. */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse badRequest(IllegalArgumentException e) {
        return new ErrorResponse(e.getMessage());
    }

    /** Our side is misconfigured or a tx failed on chain, e.g. k not set, or the derived key doesn't match. */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse serverError(IllegalStateException e) {
        log.error("Request failed", e);
        return new ErrorResponse(e.getMessage());
    }

    /** The Sepolia node couldn't be reached, or a tx wasn't mined in time. */
    @ExceptionHandler({IOException.class, TransactionException.class})
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ErrorResponse nodeError(Exception e) {
        log.error("Ethereum node error", e);
        return new ErrorResponse("Ethereum node error: " + e.getMessage());
    }
}
