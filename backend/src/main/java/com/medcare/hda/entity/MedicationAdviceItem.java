package com.medcare.hda.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 用药建议明细 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("medication_advice_item")
@Schema(description = "用药建议明细")
public class MedicationAdviceItem extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "用药建议单ID")
    private Long adviceId;

    @Schema(description = "药品ID")
    private Long medicineId;

    @Schema(description = "药品名称快照")
    private String medicineName;

    @Schema(description = "规格快照")
    private String specification;

    @Schema(description = "用法")
    private String usageMethod;

    @Schema(description = "剂量")
    private String dosage;

    @Schema(description = "频次")
    private String frequency;

    @Schema(description = "用药天数")
    private Integer durationDays;

    @Schema(description = "数量")
    private String quantity;

    @Schema(description = "注意事项")
    private String precautions;

    @Schema(description = "排序")
    private Integer sortOrder;
}
