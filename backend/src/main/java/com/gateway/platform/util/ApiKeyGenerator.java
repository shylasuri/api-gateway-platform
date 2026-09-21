package com.gateway.platform.util;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class ApiKeyGenerator {

    private static final String PREFIX = "gw_live_";
    private final SecureRandom secureRandom = new SecureRandom();

    /** Generates a new raw API key. Only returned once, never persisted. */
    public String generateRawKey() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return PREFIX + token;
    }

    public String displayPrefix(String rawKey) {
        int len = Math.min(rawKey.length(), 14);
        return rawKey.substring(0, len) + "...";
    }

    /** SHA-256 hash of the raw key — this, not the raw key, is what gets stored/queried. */
    public String hash(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
