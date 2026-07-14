package com.medcare.hda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "Login response")
public class LoginVO {
    @Schema(description = "JWT access token")
    private String token;
    @Schema(description = "JWT refresh token")
    private String refreshToken;
    @Schema(description = "Access token lifetime in milliseconds")
    private Long expiresIn;
    @Schema(description = "User ID")
    private Long userId;
    @Schema(description = "Username")
    private String username;
    @Schema(description = "Nickname")
    private String nickname;
    @Schema(description = "Default active role")
    private String role;
    @Schema(description = "All roles")
    private List<String> roles;
    @Schema(description = "Avatar URL")
    private String avatar;
}
