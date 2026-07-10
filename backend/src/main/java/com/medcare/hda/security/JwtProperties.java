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
    private Long expiration;
    private String header = "Authorization";
    private String tokenPrefix = "Bearer ";
}
