package com.medcare.hda.service;

import com.medcare.hda.common.MetricRules;
import com.medcare.hda.dto.AlertAnalysisItemVO;
import com.medcare.hda.dto.AlertAnalysisVO;
import com.medcare.hda.entity.HealthMetric;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 纯规则分析引擎，不读写数据库，便于预览与生成共用同一结果。 */
@Component
public class AlertAnalysisEngine {

    public AlertAnalysisVO analyze(Long userId, List<HealthMetric> source,
                                   LocalDateTime windowStart, LocalDateTime windowEnd) {
        List<HealthMetric> metrics = source.stream()
                .sorted(Comparator.comparing(HealthMetric::getMeasureTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        AlertAnalysisVO result = new AlertAnalysisVO();
        result.setWindowStart(windowStart);
        result.setWindowEnd(windowEnd);
        result.setMetricCount(metrics.size());

        Map<String, List<JudgedMetric>> abnormalByType = new LinkedHashMap<>();
        int analyzable = 0;
        int abnormal = 0;
        for (HealthMetric metric : metrics) {
            if (!MetricRules.supports(metric.getMetricType()) || metric.getMetricValue() == null) {
                continue;
            }
            analyzable++;
            MetricRules.Judge judge = MetricRules.judge(metric);
            if (judge.abnormal()) {
                abnormal++;
                abnormalByType.computeIfAbsent(metric.getMetricType(), key -> new ArrayList<>())
                        .add(new JudgedMetric(metric, judge));
            }
        }
        result.setAnalyzableMetricCount(analyzable);
        result.setAbnormalCount(abnormal);

        abnormalByType.forEach((type, entries) -> {
            JudgedMetric latest = entries.getFirst();
            JudgedMetric highest = entries.stream()
                    .filter(item -> "HIGH".equals(item.judge().level()))
                    .findFirst()
                    .orElse(latest);
            String level = entries.stream().anyMatch(item -> "HIGH".equals(item.judge().level()))
                    ? "HIGH" : "MEDIUM";
            if ("HIGH".equals(level)) {
                result.setHighRiskCount(result.getHighRiskCount() + 1);
            } else {
                result.setMediumRiskCount(result.getMediumRiskCount() + 1);
            }
            result.getAbnormalTypes().add(new AlertAnalysisItemVO(
                    type,
                    MetricRules.typeName(type),
                    entries.size(),
                    level,
                    latest.judge().message(),
                    highest.judge().message(),
                    fingerprint(userId, type, entries)
            ));
        });

        if (analyzable == 0) {
            result.setConclusion("近7日暂无可分析的血压、血糖、心率或体温记录");
        } else if (abnormal == 0) {
            result.setConclusion("近7日可分析体征均未触发预警阈值");
        } else {
            result.setConclusion("发现 " + abnormal + " 条异常体征，涉及 "
                    + abnormalByType.size() + " 类指标");
        }
        return result;
    }

    private String fingerprint(Long userId, String type, List<JudgedMetric> entries) {
        StringBuilder raw = new StringBuilder().append(userId).append('|').append(type);
        entries.stream()
                .sorted(Comparator.comparing(item -> item.metric().getId(), Comparator.nullsLast(Long::compareTo)))
                .forEach(item -> raw.append('|')
                        .append(item.metric().getId()).append(':')
                        .append(item.metric().getMetricValue()).append(':')
                        .append(item.metric().getMetricValue2()).append(':')
                        .append(item.metric().getMeasureTime()).append(':')
                        .append(item.metric().getUpdateTime()));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(raw.toString().getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JVM 不支持 SHA-256", e);
        }
    }

    private record JudgedMetric(HealthMetric metric, MetricRules.Judge judge) {}
}
