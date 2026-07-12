package com.medcare.hda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "医生注册请求")
public class DoctorRegisterDTO {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度 3-20")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(max = 512, message = "密码格式不正确")
    private String password;

    @NotBlank(message = "姓名不能为空")
    private String name;

    private String phone;

    @NotBlank(message = "医院不能为空")
    private String hospital;

    @NotBlank(message = "科室不能为空")
    private String department;

    @NotBlank(message = "职称不能为空")
    private String title;

    private String speciality;

    private String introduction;
}
