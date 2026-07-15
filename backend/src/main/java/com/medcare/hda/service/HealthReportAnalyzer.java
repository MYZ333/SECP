package com.medcare.hda.service;

import com.medcare.hda.common.MetricRules;
import com.medcare.hda.dto.HealthReportDetailVO;
import com.medcare.hda.entity.HealthMetric;
import com.medcare.hda.entity.HealthProfile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 纯确定性健康体征分析器，不执行持久化或 AI 调用。 */
@Component
public class HealthReportAnalyzer {

    static final int MAX_CHART_POINTS = 120;
    private static final Map<String, Double> TREND_THRESHOLDS = Map.of(
            "BLOOD_PRESSURE", 5.0,
            "BLOOD_SUGAR", 0.5,
            "HEART_RATE", 5.0,
            "TEMPERATURE", 0.3,
            "WEIGHT", 1.0
    );

    public HealthReportDetailVO analyze(List<HealthMetric> rawMetrics, HealthProfile profile,
                                        LocalDateTime periodStart, LocalDateTime periodEnd) {
        List<HealthMetric> nonNull = rawMetrics.stream()
                .filter(m -> m.getMetricValue() != null && m.getMetricType() != null && m.getMeasureTime() != null)
                .sorted(Comparator.comparing(HealthMetric::getMeasureTime))
                .toList();
        int invalidCount = rawMetrics.size() - nonNull.size();

        Map<String, HealthMetric> uniqueMap = new LinkedHashMap<>();
        for (HealthMetric metric : nonNull) uniqueMap.putIfAbsent(dedupKey(metric), metric);
        List<HealthMetric> metrics = new ArrayList<>(uniqueMap.values());
        int duplicateCount = nonNull.size() - metrics.size();

        Map<String, List<HealthMetric>> grouped = metrics.stream().collect(Collectors.groupingBy(
                HealthMetric::getMetricType, LinkedHashMap::new, Collectors.toList()));
        List<HealthReportDetailVO.MetricAnalysis> analyses = new ArrayList<>();
        List<HealthReportDetailVO.AbnormalEvent> abnormalEvents = new ArrayList<>();
        List<String> riskReasons = new ArrayList<>();

        for (Map.Entry<String, List<HealthMetric>> entry : grouped.entrySet()) {
            HealthReportDetailVO.MetricAnalysis analysis = analyzeMetric(entry.getKey(), entry.getValue(), abnormalEvents);
            analyses.add(analysis);
            if (!"NORMAL".equals(analysis.getRiskLevel()) && !"INSUFFICIENT".equals(analysis.getRiskLevel())) {
                riskReasons.add(analysis.getMetricName() + "：" + riskReason(analysis));
            }
        }

        String overallRisk = overallRisk(analyses, metrics.size());
        long missingBpSecondary = metrics.stream().filter(m -> "BLOOD_PRESSURE".equals(m.getMetricType())
                && m.getMetricValue2() == null).count();
        double qualityNumerator = Math.max(0, metrics.size() - missingBpSecondary * 0.5);
        double dataQuality = rawMetrics.isEmpty() ? 0 : round(qualityNumerator / rawMetrics.size(), 3);

        return HealthReportDetailVO.builder()
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .riskLevel(overallRisk)
                .algorithmVersion(HealthReportGenerationService.ALGORITHM_VERSION)
                .dataCount(metrics.size())
                .invalidCount(invalidCount)
                .duplicateCount(duplicateCount)
                .dataQuality(dataQuality)
                .legacy(false)
                .metrics(analyses)
                .abnormalEvents(abnormalEvents.stream()
                        .sorted(Comparator.comparing(HealthReportDetailVO.AbnormalEvent::getTime).reversed()).toList())
                .riskReasons(riskReasons)
                .profile(profileContext(profile))
                .build();
    }

    private HealthReportDetailVO.MetricAnalysis analyzeMetric(String type, List<HealthMetric> rows,
                                                               List<HealthReportDetailVO.AbnormalEvent> abnormalEvents) {
        List<Double> primaryValues = rows.stream().map(HealthMetric::getMetricValue).toList();
        List<Double> secondaryValues = rows.stream().map(HealthMetric::getMetricValue2).filter(java.util.Objects::nonNull).toList();
        int abnormalCount = 0;
        int consecutive = 0;
        int maxConsecutive = 0;
        boolean high = false;

        for (HealthMetric row : rows) {
            MetricRules.Judge judge = MetricRules.judge(row);
            if (judge.abnormal()) {
                abnormalCount++;
                consecutive++;
                maxConsecutive = Math.max(maxConsecutive, consecutive);
                high |= "HIGH".equals(judge.level());
                abnormalEvents.add(HealthReportDetailVO.AbnormalEvent.builder()
                        .metricType(type).metricName(MetricRules.typeName(type)).time(row.getMeasureTime())
                        .value(row.getMetricValue()).value2(row.getMetricValue2()).unit(row.getUnit())
                        .level(judge.level()).message(judge.message()).build());
            } else {
                consecutive = 0;
            }
        }

        double abnormalRate = rows.isEmpty() ? 0 : round((double) abnormalCount / rows.size(), 4);
        String risk = high ? "HIGH" : abnormalCount == 0 ? "NORMAL"
                : (abnormalRate > 0.30 || maxConsecutive >= 2) ? "WARNING" : "ATTENTION";
        String primaryTrend = trend(rows, HealthMetric::getMetricValue, TREND_THRESHOLDS.getOrDefault(type, 1.0));
        String secondaryTrend = "BLOOD_PRESSURE".equals(type)
                ? trend(rows.stream().filter(r -> r.getMetricValue2() != null).toList(), HealthMetric::getMetricValue2, 3.0)
                : null;

        return HealthReportDetailVO.MetricAnalysis.builder()
                .metricType(type).metricName(MetricRules.typeName(type)).unit(rows.get(rows.size() - 1).getUnit())
                .validCount(rows.size())
                .missingSecondaryCount("BLOOD_PRESSURE".equals(type) ? rows.size() - secondaryValues.size() : 0)
                .primary(stats(primaryValues))
                .secondary(secondaryValues.isEmpty() ? null : stats(secondaryValues))
                .abnormalCount(abnormalCount).abnormalRate(abnormalRate)
                .maxConsecutiveAbnormal(maxConsecutive)
                .trend(primaryTrend).secondaryTrend(secondaryTrend).riskLevel(risk)
                .chartPoints(chartPoints(rows))
                .build();
    }

    private HealthReportDetailVO.NumberStats stats(List<Double> values) {
        List<Double> sorted = values.stream().sorted().toList();
        int size = sorted.size();
        double median = size % 2 == 1 ? sorted.get(size / 2)
                : (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2.0;
        return HealthReportDetailVO.NumberStats.builder()
                .latest(values.get(values.size() - 1))
                .average(round(values.stream().mapToDouble(Double::doubleValue).average().orElse(0), 2))
                .median(round(median, 2)).minimum(sorted.get(0)).maximum(sorted.get(size - 1)).build();
    }

    private String trend(List<HealthMetric> rows, Function<HealthMetric, Double> valueGetter, double threshold) {
        if (rows.size() < 3 || rows.stream().map(m -> m.getMeasureTime().toLocalDate()).distinct().count() < 3) {
            return "INSUFFICIENT";
        }
        int segmentSize = Math.max(1, rows.size() / 3);
        double first = median(rows.subList(0, segmentSize).stream().map(valueGetter).toList());
        double last = median(rows.subList(rows.size() - segmentSize, rows.size()).stream().map(valueGetter).toList());
        double delta = last - first;
        if (Math.abs(delta) < threshold) return "STABLE";
        return delta > 0 ? "UP" : "DOWN";
    }

    private double median(List<Double> values) {
        List<Double> sorted = values.stream().sorted().toList();
        int size = sorted.size();
        return size % 2 == 1 ? sorted.get(size / 2) : (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2.0;
    }

    private List<HealthReportDetailVO.ChartPoint> chartPoints(List<HealthMetric> rows) {
        Set<Integer> selected = new LinkedHashSet<>();
        if (rows.size() <= MAX_CHART_POINTS) {
            for (int i = 0; i < rows.size(); i++) selected.add(i);
        } else {
            Set<Integer> required = new LinkedHashSet<>();
            for (int i = 0; i < rows.size(); i++) {
                if (MetricRules.judge(rows.get(i)).abnormal()) required.add(i);
            }
            required.add(indexOf(rows, true, false));
            required.add(indexOf(rows, false, false));
            required.add(indexOf(rows, true, true));
            required.add(indexOf(rows, false, true));
            required.add(rows.size() - 1);
            required.remove(-1);
            // 异常点极多时，完整异常列表仍保存在 abnormalEvents；图表优先保留极值、最新值和最近异常。
            if (required.size() > MAX_CHART_POINTS) {
                selected.add(indexOf(rows, true, false));
                selected.add(indexOf(rows, false, false));
                selected.add(indexOf(rows, true, true));
                selected.add(indexOf(rows, false, true));
                selected.add(rows.size() - 1);
                List<Integer> recentRequired = required.stream().sorted(Comparator.reverseOrder()).toList();
                for (Integer index : recentRequired) {
                    if (selected.size() >= MAX_CHART_POINTS) break;
                    selected.add(index);
                }
            } else {
                selected.addAll(required);
            }
            for (int i = 0; selected.size() < MAX_CHART_POINTS && i < MAX_CHART_POINTS * 3; i++) {
                selected.add((int) Math.round((double) i * (rows.size() - 1) / (MAX_CHART_POINTS * 3 - 1)));
            }
        }
        return selected.stream().filter(i -> i >= 0).sorted().limit(MAX_CHART_POINTS)
                .map(i -> toPoint(rows.get(i))).toList();
    }

    private int indexOf(List<HealthMetric> rows, boolean max, boolean secondary) {
        return java.util.stream.IntStream.range(0, rows.size())
                .filter(i -> !secondary || rows.get(i).getMetricValue2() != null)
                .boxed().min((a, b) -> {
                    double av = secondary ? rows.get(a).getMetricValue2() : rows.get(a).getMetricValue();
                    double bv = secondary ? rows.get(b).getMetricValue2() : rows.get(b).getMetricValue();
                    return max ? Double.compare(bv, av) : Double.compare(av, bv);
                }).orElse(-1);
    }

    private HealthReportDetailVO.ChartPoint toPoint(HealthMetric row) {
        MetricRules.Judge judge = MetricRules.judge(row);
        return HealthReportDetailVO.ChartPoint.builder().time(row.getMeasureTime())
                .value(row.getMetricValue()).value2(row.getMetricValue2())
                .abnormal(judge.abnormal()).level(judge.level()).build();
    }

    private String overallRisk(List<HealthReportDetailVO.MetricAnalysis> analyses, int dataCount) {
        if (analyses.stream().anyMatch(a -> "HIGH".equals(a.getRiskLevel()))) return "HIGH";
        if (analyses.stream().anyMatch(a -> "WARNING".equals(a.getRiskLevel()))) return "WARNING";
        if (analyses.stream().anyMatch(a -> "ATTENTION".equals(a.getRiskLevel()))) return "ATTENTION";
        if (dataCount < 3 || analyses.stream().allMatch(a -> "INSUFFICIENT".equals(a.getTrend()))) return "INSUFFICIENT";
        return "NORMAL";
    }

    private String riskReason(HealthReportDetailVO.MetricAnalysis analysis) {
        if ("HIGH".equals(analysis.getRiskLevel())) return "出现高级别异常测量值";
        if (analysis.getMaxConsecutiveAbnormal() >= 2) return "连续异常达到 " + analysis.getMaxConsecutiveAbnormal() + " 次";
        if (analysis.getAbnormalRate() > 0.30) return String.format("异常率为 %.1f%%", analysis.getAbnormalRate() * 100);
        return "存在偶发异常测量值";
    }

    private HealthReportDetailVO.ProfileContext profileContext(HealthProfile profile) {
        if (profile == null) return null;
        Double bmi = null;
        if (profile.getHeight() != null && profile.getHeight() > 0 && profile.getWeight() != null) {
            double meters = profile.getHeight() / 100.0;
            bmi = round(profile.getWeight() / (meters * meters), 1);
        }
        return HealthReportDetailVO.ProfileContext.builder().height(profile.getHeight())
                .weight(profile.getWeight()).bmi(bmi).build();
    }

    private String dedupKey(HealthMetric metric) {
        return metric.getMetricType() + "|" + metric.getMeasureTime() + "|" + metric.getMetricValue() + "|" + metric.getMetricValue2();
    }

    private double round(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }
}
