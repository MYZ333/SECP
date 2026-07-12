package com.medcare.hda.security;

import cn.hutool.core.util.IdUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/** JWT 生成与解析工具（access + refresh 双令牌，带 jti 便于登出黑名单） */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;

    /** token 类型声明 */
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";
    private static final String CLAIM_TYPE = "type";

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /** 生成 access token（短寿命，承载身份） */
    public String generateToken(Long userId, String username, String role) {
        return buildToken(userId, username, role, TYPE_ACCESS,
                jwtProperties.getExpiration(), IdUtil.fastSimpleUUID());
    }

    /** 生成 refresh token（长寿命，仅用于换取新 access token） */
    public String generateRefreshToken(Long userId, String username, String role) {
        return buildToken(userId, username, role, TYPE_REFRESH,
                jwtProperties.getRefreshExpiration(), IdUtil.fastSimpleUUID());
    }

    private String buildToken(Long userId, String username, String role,
                              String type, long ttlMillis, String jti) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);
        claims.put(CLAIM_TYPE, type);
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ttlMillis);
        return Jwts.builder()
                .claims(claims)
                .id(jti)
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getKey())
                .compact();
    }

    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.debug("JWT 解析失败: {}", e.getMessage());
            return null;
        }
    }

    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        return claims == null ? null : claims.get("userId", Long.class);
    }

    /** 取 token 的唯一标识 jti（用于黑名单） */
    public String getJti(String token) {
        Claims claims = parseToken(token);
        return claims == null ? null : claims.getId();
    }

    /** 是否为 refresh 类型 token */
    public boolean isRefreshToken(Claims claims) {
        return claims != null && TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class));
    }

    /** token 剩余有效毫秒数（用于黑名单 TTL），已过期或非法返回 0 */
    public long getRemainingMillis(Claims claims) {
        if (claims == null || claims.getExpiration() == null) {
            return 0;
        }
        long remaining = claims.getExpiration().getTime() - System.currentTimeMillis();
        return Math.max(remaining, 0);
    }

    public boolean isValid(String token) {
        Claims claims = parseToken(token);
        return claims != null && claims.getExpiration().after(new Date());
    }
}
