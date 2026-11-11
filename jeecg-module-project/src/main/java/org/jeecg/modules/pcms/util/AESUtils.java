package org.jeecg.modules.pcms.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Slf4j
public class AESUtils {
    private static final String ALGORITHM = "AES";
    private static final int KEY_SIZE = 128;

    // 生成AES密钥
    public static SecretKey generateSecretKey() {
        try {
            log.info("------------------------ 开始生成秘钥 ------------------------");
            KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
            keyGen.init(KEY_SIZE);
            SecretKey secretKey = keyGen.generateKey();
            // 将密钥以 Base64 格式输出到日志
            byte[] encoded = secretKey.getEncoded();
            String encodedKey = Base64.getEncoder().encodeToString(encoded);
            log.info("------------------------ 生成秘钥 (Base64): " + encodedKey + "------------------------");
            return secretKey;
        } catch (Exception e) {
            log.error("------------------------ 生成秘钥错误" + e.getMessage() + " ------------------------");
            return null;
        }
    }

    // 加密
    public static String encrypt(String data, SecretKey key) {
        try {
            log.info("------------------------ 开始加密 ------------------------");
            byte[] bytes = data.getBytes();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encryptedData = cipher.doFinal(bytes);
            String str = Base64.getEncoder().encodeToString(encryptedData);
            log.info("------------------------ 加密秘钥: " + str + "------------------------");
            return str;
        } catch (Exception e) {
            log.error("------------------------ 加密错误" + e.getMessage() + " ------------------------");
            return null;
        }
    }

    // 解密
    public static String decrypt(String encryptedData, SecretKey key) {
        try {
            log.info("------------------------ 开始解密 ------------------------");
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decodedData = Base64.getDecoder().decode(encryptedData);
            byte[] decryptedData = cipher.doFinal(decodedData);
            String str = new String(decryptedData);
            log.info("------------------------ 加密秘钥: " + str + "------------------------");
            return str;
        } catch (Exception e) {
            log.error("------------------------ 解密错误" + e.getMessage() + " ------------------------");
            return null;
        }
    }

    // 将 Base64 编码的密钥字符串转回 SecretKey 对象
    public static SecretKey decodeKeyFromBase64(String encodedKey) {
        byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
        // 使用AES算法创建密钥
        SecretKeySpec aes = new SecretKeySpec(decodedKey, "AES");
        return aes;
    }

    // 将 SecretKey 对象 转换为 Base64
    public static String encryptKeyFromBase64(SecretKey secretKey) {
        // 将密钥转换为 Base64 格式
        byte[] encoded = secretKey.getEncoded();
        String encodedKey = Base64.getEncoder().encodeToString(encoded);
        return encodedKey;
    }

}

