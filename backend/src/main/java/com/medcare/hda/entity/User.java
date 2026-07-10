package com.medcare.hda.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/** 用户表 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
@Schema(description = "用户")
public class User extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "用户名/登录账号")
    private String username;

    @Schema(description = "密码(BCrypt 加密存储)")
    private String password;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像 URL")
    private String avatar;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "性别: 0 未知 1 男 2 女")
    private Integer gender;

    @Schema(description = "生日")
    private LocalDate birthday;

    @Schema(description = "角色: USER 普通用户 / ADMIN 管理员")
    private String role;

    @Schema(description = "积分余额")
    private Integer points;

    @Schema(description = "状态: 0 正常 1 禁用")
    private Integer status;
}
