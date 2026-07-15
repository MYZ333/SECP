package com.medcare.hda.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 单类体征的近七日异常分析结果。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertAnalysisItemVO {
    private String metricType;
    private String typeName;
    private int abnormalCount;
    private String level;
    private String latestMessage;
    private String highestRiskMessage;
    @JsonIgnore
    private String generationKey;
}
