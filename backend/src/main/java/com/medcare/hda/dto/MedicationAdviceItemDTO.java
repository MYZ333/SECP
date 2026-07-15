package com.medcare.hda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "用药建议明细请求")
public class MedicationAdviceItemDTO {

    @NotNull(message = "请选择药品")
    @Schema(description = "药品ID")
    private Long medicineId;

    @Size(max = 100, message = "用法不能超过100字")
    @Schema(description = "用法")
    private String usageMethod;

    @Size(max = 100, message = "剂量不能超过100字")
    @Schema(description = "剂量")
    private String dosage;

    @Size(max = 100, message = "频次不能超过100字")
    @Schema(description = "频次")
    private String frequency;

    @Schema(description = "用药天数")
    private Integer durationDays;

    @Size(max = 100, message = "数量不能超过100字")
    @Schema(description = "数量")
    private String quantity;

    @Size(max = 1000, message = "注意事项不能超过1000字")
    @Schema(description = "注意事项")
    private String precautions;
}
