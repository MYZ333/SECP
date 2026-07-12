package com.medcare.hda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "注册请求")
public class RegisterDTO {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度 3-20")
    @Schema(description = "用户名")
    private String username;

    @NotBlank(message = "密码不能为空")
    // 兼容 RSA 加密传输（密文较长）；明文 6-32 位的真实校验在解密后由服务层执行
    @Size(max = 512, message = "密码格式不正确")
    @Schema(description = "密码(支持前端 RSA 加密后传输)")
    private String password;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "手机号")
    private String phone;
}
