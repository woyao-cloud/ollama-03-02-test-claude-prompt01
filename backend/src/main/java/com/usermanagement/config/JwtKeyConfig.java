package com.usermanagement.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.util.Base64;

/**
 * JWT Key Configuration
 * Handles RSA key pair generation and loading
 *
 * @author Config Team
 * @since 1.0
 */
@Configuration
public class JwtKeyConfig {

    private static final Logger logger = LoggerFactory.getLogger(JwtKeyConfig.class);

    private static final String KEYS_DIR = "src/main/resources/keys";
    private static final String PRIVATE_KEY_FILE = "app.key";
    private static final String PUBLIC_KEY_FILE = "app.pub";

    /**
     * Generate or load RSA key pair
     */
    @Bean
    public KeyPair jwtKeyPair() throws Exception {
        Path privateKeyPath = Paths.get(KEYS_DIR, PRIVATE_KEY_FILE);
        Path publicKeyPath = Paths.get(KEYS_DIR, PUBLIC_KEY_FILE);

        // Check if keys exist
        if (Files.exists(privateKeyPath) && Files.exists(publicKeyPath)) {
            logger.info("Loading existing RSA key pair from files");
            return loadKeyPair(privateKeyPath, publicKeyPath);
        }

        // Generate new keys
        logger.info("Generating new RSA key pair");
        KeyPair keyPair = generateKeyPair();

        // Save keys to files
        saveKeyPair(keyPair, privateKeyPath, publicKeyPath);

        return keyPair;
    }

    /**
     * Generate RSA key pair
     */
    private KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        return keyGen.generateKeyPair();
    }

    /**
     * Load key pair from files
     */
    private KeyPair loadKeyPair(Path privateKeyPath, Path publicKeyPath) throws Exception {
        byte[] privateKeyBytes = Base64.getDecoder().decode(
                Files.readString(privateKeyPath, StandardCharsets.UTF_8)
                        .replaceAll("-----BEGIN PRIVATE KEY-----", "")
                        .replaceAll("-----END PRIVATE KEY-----", "")
                        .replaceAll("\\s", "")
        );

        byte[] publicKeyBytes = Base64.getDecoder().decode(
                Files.readString(publicKeyPath, StandardCharsets.UTF_8)
                        .replaceAll("-----BEGIN PUBLIC KEY-----", "")
                        .replaceAll("-----END PUBLIC KEY-----", "")
                        .replaceAll("\\s", "")
        );

        java.security.spec.PKCS8EncodedKeySpec privateSpec =
                new java.security.spec.PKCS8EncodedKeySpec(privateKeyBytes);
        java.security.spec.X509EncodedKeySpec publicSpec =
                new java.security.spec.X509EncodedKeySpec(publicKeyBytes);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = keyFactory.generatePrivate(privateSpec);
        PublicKey publicKey = keyFactory.generatePublic(publicSpec);

        return new KeyPair(publicKey, privateKey);
    }

    /**
     * Save key pair to files
     */
    private void saveKeyPair(KeyPair keyPair, Path privateKeyPath, Path publicKeyPath) throws IOException {
        // Create directory if not exists
        Files.createDirectories(privateKeyPath.getParent());

        // Encode keys to Base64
        String privateKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
        String publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

        // Format as PEM
        String privateKeyPem = formatPem(privateKeyBase64, "PRIVATE KEY");
        String publicKeyPem = formatPem(publicKeyBase64, "PUBLIC KEY");

        // Write to files
        Files.writeString(privateKeyPath, privateKeyPem, StandardCharsets.UTF_8);
        Files.writeString(publicKeyPath, publicKeyPem, StandardCharsets.UTF_8);

        logger.info("RSA key pair saved to files");
    }

    /**
     * Format Base64 string as PEM
     */
    private String formatPem(String base64, String type) {
        StringBuilder pem = new StringBuilder();
        pem.append("-----BEGIN ").append(type).append("-----\n");

        // Insert line breaks every 64 characters
        for (int i = 0; i < base64.length(); i += 64) {
            pem.append(base64, i, Math.min(i + 64, base64.length())).append("\n");
        }

        pem.append("-----END ").append(type).append("-----\n");
        return pem.toString();
    }
}
