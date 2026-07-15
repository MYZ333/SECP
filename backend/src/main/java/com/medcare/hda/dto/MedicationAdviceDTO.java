package com.medcare.hda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "用药建议单请求")
public class MedicationAdviceDTO {

    @Size(max = 1000, message = "医生说明不能超过1000字")
    @Schema(description = "医生说明")
    private String doctorNote;

    @Valid
    @NotEmpty(message = "请至少添加一种药品")
    @Schema(description = "药品明细")
    private List<MedicationAdviceItemDTO> items;
}
