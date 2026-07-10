package com.medcare.hda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "登录返回")
public class LoginVO {
    @Schema(description = "JWT token")
    private String token;
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
