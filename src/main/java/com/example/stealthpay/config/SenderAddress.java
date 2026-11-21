package com.example.stealthpay.config;

import com.example.stealthpay.crypto.EthereumAddress;
import com.example.stealthpay.crypto.Secp256k1;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Prints the Ethereum address of the demo sender wallet, so you can fund it from a faucet.
 * Run this main() by hand. It prints ONLY the address (public). The private key is never printed.
 *
 * It reads STEALTHPAY_SENDER_PRIVATE_KEY from the environment, or else from the .env file
 * in the working directory (the project root when started from the IDE).
 */
public class SenderAddress {

    private static final String KEY_NAME = "STEALTHPAY_SENDER_PRIVATE_KEY";

    public static void main(String[] args) throws IOException {
        String privateKeyHex = Optional.ofNullable(System.getenv(KEY_NAME))
                .filter(value -> !value.isBlank())
                .orElse(readFromDotEnv());

        BigInteger privateKey = Numeric.toBigInt(privateKeyHex.trim());
        String address = EthereumAddress.fromPublicKey(Secp256k1.publicKey(privateKey, false));

        System.out.println("Sender address (paste this into the faucet):");
        System.out.println(Keys.toChecksumAddress(address));
    }

    /** Finds the sender key line in .env. Throws if the file or the line is missing. */
    private static String readFromDotEnv() throws IOException {
        Path dotEnv = Path.of(".env");
        if (!Files.exists(dotEnv)) {
            throw new IllegalStateException(KEY_NAME + " is not set and there is no .env in " + dotEnv.toAbsolutePath());
        }
        return Files.readAllLines(dotEnv).stream()
                .map(String::trim)
                .filter(line -> line.startsWith(KEY_NAME + "="))
                .map(line -> line.substring(KEY_NAME.length() + 1))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(KEY_NAME + " not found in .env"));
    }
}
