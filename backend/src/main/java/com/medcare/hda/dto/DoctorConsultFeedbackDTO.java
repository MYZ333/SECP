package com.medcare.hda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "患者咨询反馈请求")
public class DoctorConsultFeedbackDTO {

    @Schema(description = "患者评分: 1-5")
    @Min(value = 1, message = "评分不能低于1分")
    @Max(value = 5, message = "评分不能高于5分")
    private Integer rating;

    @Schema(description = "评价标签")
    @Size(max = 8, message = "评价标签不能超过8个")
    private List<@Size(max = 20, message = "单个评价标签不能超过20字") String> tags;

    @Schema(description = "文字评价")
    @Size(max = 500, message = "文字评价不能超过500字")
    private String feedback;
}
