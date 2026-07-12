package com.medcare.hda.common.crypto;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 敏感字段 AES-256-GCM 加解密。
 * - 密文带 "ENC:" 前缀，便于识别"已加密 vs 遗留明文"，读取时对明文向后兼容。
 * - 每次加密随机 12 字节 IV，防止相同明文产生相同密文。
 * - 由 Spring 装配并在 @PostConstruct 派生密钥，暴露静态方法供 MyBatis TypeHandler 使用
 *   （TypeHandler 由 MyBatis 实例化，拿不到 Spring 依赖，故用静态桥接）。
 */
@Slf4j
@Component
public class FieldCrypto {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String PREFIX = "ENC:";
    private static final int IV_LEN = 12;
    private static final int TAG_BITS = 128;

    private static volatile SecretKeySpec KEY;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Value("${hda.crypto.aes-key:}")
    private String rawKey;

    @PostConstruct
    public void init() throws Exception {
        // 任意长度输入经 SHA-256 派生成固定 256 位密钥
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(rawKey.getBytes(StandardCharsets.UTF_8));
        KEY = new SecretKeySpec(digest, "AES");
        log.info("敏感字段加密(FieldCrypto)已初始化");
    }

    /** 加密；已是密文或空则原样返回 */
    public static String encrypt(String plain) {
        if (plain == null || plain.isEmpty() || plain.startsWith(PREFIX) || KEY == null) {
            return plain;
        }
        try {
            byte[] iv = new byte[IV_LEN];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, KEY, new GCMParameterSpec(TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
            return PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("字段加密失败", e);
            return plain;
        }
    }

    /** 解密；非密文(遗留明文)或空则原样返回 */
    public static String decrypt(String stored) {
        if (stored == null || stored.isEmpty() || !stored.startsWith(PREFIX) || KEY == null) {
            return stored;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            byte[] iv = new byte[IV_LEN];
            System.arraycopy(combined, 0, iv, 0, IV_LEN);
            byte[] cipherText = new byte[combined.length - IV_LEN];
            System.arraycopy(combined, IV_LEN, cipherText, 0, cipherText.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, KEY, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("字段解密失败，返回原值", e);
            return stored;
        }
    }
}
