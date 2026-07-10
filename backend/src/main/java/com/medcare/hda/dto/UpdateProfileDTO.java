package com.medcare.hda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "修改个人资料请求")
public class UpdateProfileDTO {
    @Schema(description = "昵称")
    private String nickname;
    @Schema(description = "头像URL")
    private String avatar;
    @Schema(description = "手机号")
    private String phone;
    @Schema(description = "性别 0未知1男2女")
    private Integer gender;
    @Schema(description = "生日")
    private LocalDate birthday;
}
