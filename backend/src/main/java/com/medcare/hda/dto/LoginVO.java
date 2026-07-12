package com.medcare.hda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "登录返回")
public class LoginVO {
    @Schema(description = "JWT access token")
    private String token;
    @Schema(description = "JWT refresh token（用于换取新 access token）")
    private String refreshToken;
    @Schema(description = "access token 有效期(毫秒)")
    private Long expiresIn;
    @Schema(description = "用户ID")
    private Long userId;
    @Schema(description = "用户名")
    private String username;
    @Schema(description = "昵称")
    private String nickname;
    @Schema(description = "角色")
    private String role;
    @Schema(description = "头像")
    private String avatar;
}
