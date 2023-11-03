package com.example.gateway.commons.utils;

import org.springframework.beans.factory.annotation.Value;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;


public class AESEncryptionUtils {



    public static String encrypt(String value,String initVector, String secretKey) {
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes(StandardCharsets.UTF_8));
            SecretKeySpec sKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, sKeySpec, iv);

            byte[] encrypted = cipher.doFinal(value.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String decrypt(String encrypted,String initVector,String secretKey) {
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes(StandardCharsets.UTF_8));
            SecretKeySpec sKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, sKeySpec, iv);

            byte[] original = cipher.doFinal(Base64.getDecoder().decode(encrypted));
            return new String(original);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    private static byte[] encryptDataWithAes(byte[] plainText, byte[] aesKey, byte[] aesIv) throws Exception {
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, aesIv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec   secretKeySpec = new SecretKeySpec(aesKey, "AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, gcmSpec);
        byte[] cipherText = cipher.doFinal(plainText);

        return cipherText;
    }

    public static String encryptPayload(String payload, String key) throws Exception {
        SecureRandom r = new SecureRandom();

        byte[] ivBytes = new byte[16];
        r.nextBytes(ivBytes);

        byte[] keyBytes   = key.getBytes(StandardCharsets.UTF_8);
        byte[] inputBytes = payload.getBytes(StandardCharsets.UTF_8);
        byte[] encryptedBytes = encryptDataWithAes(inputBytes, keyBytes, ivBytes);

        byte[] cipherTextBytes = Arrays.copyOfRange(encryptedBytes, 0, payload.length());
        byte[] authTagBytes = Arrays.copyOfRange(encryptedBytes, payload.length(), encryptedBytes.length);

        String ivHex = bytesToHex(ivBytes);
        String encryptedHex = bytesToHex(cipherTextBytes);
        String authTagHex = bytesToHex(authTagBytes);

        String result = ivHex +
                ":" +
                encryptedHex +
                ":" +
                authTagHex;
        return result;
    }

}
