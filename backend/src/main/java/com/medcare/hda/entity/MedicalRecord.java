package com.medcare.hda.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.medcare.hda.common.crypto.EncryptTypeHandler;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/** 个人健康档案-就诊/用药记录 */
@Data
@EqualsAndHashCode(callSuper = true)
// autoResultMap = true 让字段级 typeHandler(加密)对查询结果也生效
@TableName(value = "medical_record", autoResultMap = true)
@Schema(description = "就诊/用药记录")
public class MedicalRecord extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "所属用户ID")
    private Long userId;

    @Schema(description = "就诊日期")
    private LocalDate visitDate;

    @Schema(description = "就诊医院")
    private String hospital;

    @Schema(description = "科室")
    private String department;

    @Schema(description = "医生姓名")
    private String doctorName;

    @Schema(description = "诊断疾病")
    @TableField(typeHandler = EncryptTypeHandler.class)
    private String diagnosis;

    @Schema(description = "处方/用药")
    @TableField(typeHandler = EncryptTypeHandler.class)
    private String prescription;

    @Schema(description = "备注")
    private String remark;
}
