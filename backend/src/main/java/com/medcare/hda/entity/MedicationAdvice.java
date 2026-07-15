package com.medcare.hda.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/** 医生用药建议单 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("medication_advice")
@Schema(description = "用药建议单")
public class MedicationAdvice extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "咨询会话ID")
    private Long sessionId;

    @Schema(description = "医生ID")
    private Long doctorId;

    @Schema(description = "患者用户ID")
    private Long userId;

    @Schema(description = "状态: PENDING_CONFIRM/CONFIRMED/CANCELLED")
    private String status;

    @Schema(description = "医生说明")
    private String doctorNote;

    @Schema(description = "患者确认时间")
    private LocalDateTime patientConfirmTime;

    @TableField(exist = false)
    @Schema(description = "药品明细")
    private List<MedicationAdviceItem> items;
}
