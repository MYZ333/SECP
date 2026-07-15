package com.medcare.hda.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.medcare.hda.entity.HealthAlert;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 近七日体征分析预览与生成结果。 */
@Data
public class AlertAnalysisVO {
    private LocalDateTime windowStart;
    private LocalDateTime windowEnd;
    private int metricCount;
    private int analyzableMetricCount;
    private int abnormalCount;
    private int highRiskCount;
    private int mediumRiskCount;
    private int generatedCount;
    private int updatedCount;
    private int duplicateCount;
    private String conclusion;
    private List<AlertAnalysisItemVO> abnormalTypes = new ArrayList<>();
    private List<HealthAlert> alerts = new ArrayList<>();

    @JsonIgnore
    public boolean hasAbnormalMetrics() {
        return abnormalCount > 0;
    }
}
