package com.medcare.hda.security;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * 密码传输 RSA 加解密。
 * 前端用公钥加密密码再提交，后端用私钥解密还原明文，再走 BCrypt。
 * 配置了固定密钥对则用配置的；否则启动自动生成（单机可用；多节点务必配置固定密钥对）。
 */
@Slf4j
@Component
public class RsaCryptoService {

    private static final String ALGORITHM = "RSA";
    /** jsencrypt 默认使用 RSA/ECB/PKCS1Padding */
    private static final String TRANSFORMATION = "RSA/ECB/PKCS1Padding";
    /** 2048 位密钥的密文固定为 256 字节，用于区分"密文 vs 明文（向后兼容）" */
    private static final int CIPHER_BYTES = 256;

    @Value("${hda.crypto.rsa-public-key:}")
    private String configuredPublicKey;
    @Value("${hda.crypto.rsa-private-key:}")
    private String configuredPrivateKey;

    private PublicKey publicKey;
    private PrivateKey privateKey;
    private String publicKeyBase64;

    @PostConstruct
    public void init() throws Exception {
        if (StringUtils.hasText(configuredPublicKey) && StringUtils.hasText(configuredPrivateKey)) {
            KeyFactory kf = KeyFactory.getInstance(ALGORITHM);
            this.publicKey = kf.generatePublic(
                    new X509EncodedKeySpec(Base64.getDecoder().decode(configuredPublicKey.trim())));
            this.privateKey = kf.generatePrivate(
                    new PKCS8EncodedKeySpec(Base64.getDecoder().decode(configuredPrivateKey.trim())));
            this.publicKeyBase64 = configuredPublicKey.trim();
            log.info("RSA 密码加密：已加载配置的固定密钥对");
        } else {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(ALGORITHM);
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            this.publicKey = pair.getPublic();
            this.privateKey = pair.getPrivate();
            this.publicKeyBase64 = Base64.getEncoder().encodeToString(publicKey.getEncoded());
            log.warn("RSA 密码加密：未配置密钥对，已自动生成（重启会变化；多节点部署请在 hda.crypto 配置固定密钥对）");
        }
    }

    /** 供前端获取的公钥（X.509/SPKI Base64，jsencrypt 可直接 setPublicKey） */
    public String getPublicKeyBase64() {
        return publicKeyBase64;
    }

    /**
     * 解析可能加密的密码：
     * - 若为 RSA 密文（Base64 解码后恰为 256 字节）则解密还原；
     * - 否则视为明文原样返回（向后兼容旧前端 / 直连接口）。
     */
    public String resolvePassword(String raw) {
        if (!StringUtils.hasText(raw)) {
            return raw;
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(raw);
        } catch (IllegalArgumentException notBase64) {
            return raw; // 不是 Base64，一定是明文
        }
        if (decoded.length != CIPHER_BYTES) {
            return raw; // 长度不符，按明文处理
        }
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            return new String(cipher.doFinal(decoded), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.debug("RSA 解密失败，按明文处理: {}", e.getMessage());
            return raw;
        }
    }
}
