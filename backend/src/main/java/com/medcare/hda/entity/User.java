package com.medcare.hda.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.medcare.hda.annotation.Desensitize;
import com.medcare.hda.common.crypto.DesensitizeType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.List;

/** Login account. Role, points, and patient demographics are loaded from related tables. */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
@Schema(description = "User account")
public class User extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "Login username")
    private String username;

    @Schema(description = "BCrypt password")
    private String password;

    @Schema(description = "Display name")
    private String nickname;

    @Schema(description = "Avatar URL")
    private String avatar;

    @Schema(description = "Phone number")
    @Desensitize(DesensitizeType.PHONE)
    private String phone;

    @TableField(exist = false)
    @Schema(description = "Gender: 0 unknown, 1 male, 2 female")
    private Integer gender;

    @TableField(exist = false)
    @Schema(description = "Birthday")
    private LocalDate birthday;

    @TableField(exist = false)
    @Schema(description = "Default active role")
    private String role;

    @TableField(exist = false)
    @Schema(description = "All role codes")
    private List<String> roles;

    @TableField(exist = false)
    @Schema(description = "Point balance")
    private Integer points;

    @Schema(description = "Status: 0 enabled, 1 disabled")
    private Integer status;
}
