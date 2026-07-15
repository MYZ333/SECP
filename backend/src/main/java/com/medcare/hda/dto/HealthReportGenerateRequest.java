package com.medcare.hda.dto;

import lombok.Data;

/** 健康报告生成参数。 */
@Data
public class HealthReportGenerateRequest {
    private Integer rangeDays = 30;
    private Boolean useAiNarrative = true;
}
