package com.medcare.hda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "忘记密码-重置密码请求")
public class ResetPasswordDTO {
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "验证码为6位数字")
    @Schema(description = "短信验证码", example = "123456")
    private String code;

    @NotBlank(message = "新密码不能为空")
    // 兼容 RSA 加密传输（密文较长）；明文 6-32 位的真实校验在解密后由服务层执行
    @Size(max = 512, message = "密码格式不正确")
    @Schema(description = "新密码(支持前端 RSA 加密后传输)")
    private String newPassword;
}
