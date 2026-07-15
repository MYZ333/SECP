package com.medcare.hda.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 健康报告结构化详情；同时是 structured_result 的稳定快照结构。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthReportDetailVO {
    private Long id;
    private String title;
    private String reportType;
    private LocalDate reportDate;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private String riskLevel;
    private String algorithmVersion;
    private String generationMode;
    private Integer dataCount;
    private Integer invalidCount;
    private Integer duplicateCount;
    private Double dataQuality;
    private Boolean legacy;
    private String content;
    private Narrative narrative;
    @Builder.Default
    private List<MetricAnalysis> metrics = new ArrayList<>();
    @Builder.Default
    private List<AbnormalEvent> abnormalEvents = new ArrayList<>();
    @Builder.Default
    private List<String> riskReasons = new ArrayList<>();
    private ProfileContext profile;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Narrative {
        private String summary;
        @Builder.Default
        private List<String> findings = new ArrayList<>();
        @Builder.Default
        private List<String> recommendations = new ArrayList<>();
        private String disclaimer;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricAnalysis {
        private String metricType;
        private String metricName;
        private String unit;
        private Integer validCount;
        private Integer missingSecondaryCount;
        private NumberStats primary;
        private NumberStats secondary;
        private Integer abnormalCount;
        private Double abnormalRate;
        private Integer maxConsecutiveAbnormal;
        private String trend;
        private String secondaryTrend;
        private String riskLevel;
        @Builder.Default
        private List<ChartPoint> chartPoints = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NumberStats {
        private Double latest;
        private Double average;
        private Double median;
        private Double minimum;
        private Double maximum;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChartPoint {
        private LocalDateTime time;
        private Double value;
        private Double value2;
        private Boolean abnormal;
        private String level;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AbnormalEvent {
        private String metricType;
        private String metricName;
        private LocalDateTime time;
        private Double value;
        private Double value2;
        private String unit;
        private String level;
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfileContext {
        private Double height;
        private Double weight;
        private Double bmi;
    }
}
