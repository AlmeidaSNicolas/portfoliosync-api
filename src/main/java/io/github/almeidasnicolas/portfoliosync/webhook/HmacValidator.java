package io.github.almeidasnicolas.portfoliosync.webhook;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class HmacValidator {
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";

    private String calculateHmac(String secret, byte[] body) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);

            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(secretKeySpec);

            byte[] hmacBytes = mac.doFinal(body);

            HexFormat hexFormat = HexFormat.of();
            String hmacHex = hexFormat.formatHex(hmacBytes);

            return hmacHex;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Could not calculate HMAC", e);
        }

    }

    public boolean isAuthentic(String secret, byte[] body, String header){
        if (header == null || !header.startsWith(SIGNATURE_PREFIX)) {
            return false;
        }

        String expectedSignature = SIGNATURE_PREFIX + calculateHmac(secret, body);
        byte[] expectedBytes = expectedSignature.getBytes(StandardCharsets.UTF_8);
        byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedBytes, headerBytes);


    }

}
