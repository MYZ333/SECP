package com.medcare.hda.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/** 个人健康档案-就诊/用药记录 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("medical_record")
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
    private String diagnosis;

    @Schema(description = "处方/用药")
    private String prescription;

    @Schema(description = "备注")
    private String remark;
}
