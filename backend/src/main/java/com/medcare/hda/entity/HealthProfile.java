package com.medcare.hda.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.medcare.hda.annotation.Desensitize;
import com.medcare.hda.common.crypto.DesensitizeType;
import com.medcare.hda.common.crypto.EncryptTypeHandler;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 个人健康档案-基本信息 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "health_profile", autoResultMap = true)
@Schema(description = "健康档案基本信息")
public class HealthProfile extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "所属用户ID")
    private Long userId;

    @Schema(description = "身高(cm)")
    private Double height;

    @Schema(description = "体重(kg)")
    private Double weight;

    @Schema(description = "血型: A/B/O/AB/未知")
    private String bloodType;

    @Schema(description = "过敏史")
    @TableField(typeHandler = EncryptTypeHandler.class)
    private String allergyHistory;

    @Schema(description = "家族病史")
    @TableField(typeHandler = EncryptTypeHandler.class)
    private String familyHistory;

    @Schema(description = "既往病史")
    @TableField(typeHandler = EncryptTypeHandler.class)
    private String pastHistory;

    @Schema(description = "紧急联系人")
    private String emergencyContact;

    @Schema(description = "紧急联系电话")
    @Desensitize(DesensitizeType.PHONE)
    private String emergencyPhone;

    @Schema(description = "备注")
    private String remark;
}
