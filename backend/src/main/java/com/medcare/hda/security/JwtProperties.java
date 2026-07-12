package com.medcare.hda.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** JWT 配置项，对应 application.yml 的 hda.jwt.* */
@Data
@Component
@ConfigurationProperties(prefix = "hda.jwt")
public class JwtProperties {
    private String secret;
    /** access token 有效期（毫秒） */
    private Long expiration;
    /** refresh token 有效期（毫秒） */
    private Long refreshExpiration = 604800000L;
    private String header = "Authorization";
    private String tokenPrefix = "Bearer ";
}
