package com.medcare.hda.service;

import com.medcare.hda.dto.HealthReportDetailVO;
import com.medcare.hda.entity.HealthMetric;
import com.medcare.hda.entity.HealthProfile;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HealthReportAnalyzerTest {

    private final HealthReportAnalyzer analyzer = new HealthReportAnalyzer();
    private final LocalDateTime end = LocalDateTime.of(2026, 7, 15, 12, 0);
    private final LocalDateTime start = end.minusDays(30);

    @Test
    void excludesNullsAndDuplicatesAndComputesQuality() {
        HealthMetric valid = metric("HEART_RATE", 70, null, end.minusDays(2));
        HealthMetric duplicate = metric("HEART_RATE", 70, null, end.minusDays(2));
        HealthMetric invalid = metric("HEART_RATE", null, null, end.minusDays(1));

        HealthReportDetailVO result = analyzer.analyze(List.of(valid, duplicate, invalid), null, start, end);

        assertThat(result.getDataCount()).isEqualTo(1);
        assertThat(result.getDuplicateCount()).isEqualTo(1);
        assertThat(result.getInvalidCount()).isEqualTo(1);
        assertThat(result.getDataQuality()).isEqualTo(0.333);
        assertThat(result.getRiskLevel()).isEqualTo("INSUFFICIENT");
    }

    @Test
    void calculatesMedianAndSeparateBloodPressureStatistics() {
        List<HealthMetric> rows = List.of(
                metric("BLOOD_PRESSURE", 120, 70.0, end.minusDays(5)),
                metric("BLOOD_PRESSURE", 130, 80.0, end.minusDays(3)),
                metric("BLOOD_PRESSURE", 140, 90.0, end.minusDays(1))
        );

        HealthReportDetailVO result = analyzer.analyze(rows, null, start, end);
        HealthReportDetailVO.MetricAnalysis bloodPressure = result.getMetrics().get(0);

        assertThat(bloodPressure.getPrimary().getMedian()).isEqualTo(130);
        assertThat(bloodPressure.getPrimary().getMinimum()).isEqualTo(120);
        assertThat(bloodPressure.getPrimary().getMaximum()).isEqualTo(140);
        assertThat(bloodPressure.getSecondary().getAverage()).isEqualTo(80);
        assertThat(bloodPressure.getSecondary().getMinimum()).isEqualTo(70);
        assertThat(bloodPressure.getSecondary().getMaximum()).isEqualTo(90);
        assertThat(bloodPressure.getTrend()).isEqualTo("UP");
        assertThat(bloodPressure.getSecondaryTrend()).isEqualTo("UP");
    }

    @Test
    void marksMissingBloodPressureSecondaryValueInQuality() {
        List<HealthMetric> rows = List.of(
                metric("BLOOD_PRESSURE", 120, 75.0, end.minusDays(4)),
                metric("BLOOD_PRESSURE", 122, null, end.minusDays(2)),
                metric("BLOOD_PRESSURE", 123, 77.0, end.minusDays(1))
        );

        HealthReportDetailVO result = analyzer.analyze(rows, null, start, end);

        assertThat(result.getMetrics().get(0).getMissingSecondaryCount()).isEqualTo(1);
        assertThat(result.getDataQuality()).isEqualTo(0.833);
    }

    @Test
    void appliesTrendThresholdAndRequiresThreeMeasurementDays() {
        List<HealthMetric> stable = List.of(
                metric("HEART_RATE", 70, null, end.minusDays(4)),
                metric("HEART_RATE", 72, null, end.minusDays(2)),
                metric("HEART_RATE", 74, null, end.minusDays(1))
        );
        List<HealthMetric> insufficient = List.of(
                metric("HEART_RATE", 70, null, end.minusDays(1).withHour(8)),
                metric("HEART_RATE", 80, null, end.minusDays(1).withHour(12)),
                metric("HEART_RATE", 90, null, end.withHour(8))
        );

        assertThat(analyzer.analyze(stable, null, start, end).getMetrics().get(0).getTrend()).isEqualTo("STABLE");
        assertThat(analyzer.analyze(insufficient, null, start, end).getMetrics().get(0).getTrend()).isEqualTo("INSUFFICIENT");
    }

    @Test
    void riskUsesHighBeforeWarningAndAttention() {
        List<HealthMetric> high = List.of(
                metric("HEART_RATE", 130, null, end.minusDays(3)),
                metric("HEART_RATE", 80, null, end.minusDays(2)),
                metric("HEART_RATE", 82, null, end.minusDays(1))
        );
        assertThat(analyzer.analyze(high, null, start, end).getRiskLevel()).isEqualTo("HIGH");

        List<HealthMetric> warning = List.of(
                metric("HEART_RATE", 105, null, end.minusDays(4)),
                metric("HEART_RATE", 106, null, end.minusDays(3)),
                metric("HEART_RATE", 80, null, end.minusDays(2)),
                metric("HEART_RATE", 82, null, end.minusDays(1))
        );
        HealthReportDetailVO warningResult = analyzer.analyze(warning, null, start, end);
        assertThat(warningResult.getRiskLevel()).isEqualTo("WARNING");
        assertThat(warningResult.getMetrics().get(0).getMaxConsecutiveAbnormal()).isEqualTo(2);
        assertThat(warningResult.getMetrics().get(0).getAbnormalRate()).isEqualTo(0.5);

        List<HealthMetric> attention = new ArrayList<>();
        attention.add(metric("HEART_RATE", 105, null, end.minusDays(5)));
        attention.add(metric("HEART_RATE", 80, null, end.minusDays(4)));
        attention.add(metric("HEART_RATE", 81, null, end.minusDays(3)));
        attention.add(metric("HEART_RATE", 82, null, end.minusDays(2)));
        attention.add(metric("HEART_RATE", 83, null, end.minusDays(1)));
        assertThat(analyzer.analyze(attention, null, start, end).getRiskLevel()).isEqualTo("ATTENTION");
    }

    @Test
    void computesBmiWithoutUsingMedicalHistory() {
        HealthProfile profile = new HealthProfile();
        profile.setHeight(170.0);
        profile.setWeight(68.0);
        profile.setPastHistory("不应进入结构化报告");
        List<HealthMetric> rows = List.of(
                metric("WEIGHT", 68, null, end.minusDays(3)),
                metric("WEIGHT", 68.2, null, end.minusDays(2)),
                metric("WEIGHT", 68.1, null, end.minusDays(1))
        );

        HealthReportDetailVO result = analyzer.analyze(rows, profile, start, end);

        assertThat(result.getProfile().getBmi()).isEqualTo(23.5);
        assertThat(result.toString()).doesNotContain("不应进入结构化报告");
    }

    private HealthMetric metric(String type, Number value, Number value2, LocalDateTime time) {
        HealthMetric metric = new HealthMetric();
        metric.setMetricType(type);
        metric.setMetricValue(value == null ? null : value.doubleValue());
        metric.setMetricValue2(value2 == null ? null : value2.doubleValue());
        metric.setMeasureTime(time);
        metric.setUnit(switch (type) {
            case "BLOOD_PRESSURE" -> "mmHg";
            case "HEART_RATE" -> "次/分";
            case "WEIGHT" -> "kg";
            default -> "";
        });
        return metric;
    }
}
