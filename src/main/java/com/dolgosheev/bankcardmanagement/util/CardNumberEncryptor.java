package com.dolgosheev.bankcardmanagement.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;

@Component
public class CardNumberEncryptor {

    private static final String ALGORITHM = "AES";
    
    private final Key key;

    public CardNumberEncryptor(@Value("${app.card.number.encryption.key}") String encryptionKey) {
        // Ensure the key is 16, 24, or 32 bytes long (for AES-128, AES-192, or AES-256)
        byte[] keyBytes = encryptionKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            // Use first 16 bytes if the key length is incorrect
            byte[] adjustedKey = new byte[16];
            System.arraycopy(keyBytes, 0, adjustedKey, 0, Math.min(keyBytes.length, 16));
            keyBytes = adjustedKey;
        }
        this.key = new SecretKeySpec(keyBytes, ALGORITHM);
    }

    public String encrypt(String cardNumber) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encryptedBytes = cipher.doFinal(cardNumber.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting card number", e);
        }
    }

    public String decrypt(String encryptedCardNumber) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedCardNumber));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting card number", e);
        }
    }
    
    public static String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return cardNumber;
        }
        
        int length = cardNumber.length();
        String lastFourDigits = cardNumber.substring(length - 4);
        StringBuilder masked = new StringBuilder();
        
        for (int i = 0; i < length - 4; i++) {
            if (i > 0 && i % 4 == 0) {
                masked.append(" ");
            }
            masked.append("*");
        }
        
        return masked + " " + lastFourDigits;
    }
} 