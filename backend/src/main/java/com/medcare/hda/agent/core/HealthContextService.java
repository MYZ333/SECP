package com.medcare.hda.agent.core;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.medcare.hda.common.MetricRules;
import com.medcare.hda.entity.HealthMetric;
import com.medcare.hda.entity.HealthProfile;
import com.medcare.hda.entity.MedicalRecord;
import com.medcare.hda.mapper.HealthMetricMapper;
import com.medcare.hda.mapper.HealthProfileMapper;
import com.medcare.hda.mapper.MedicalRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HealthContextService {
    private static final Duration HIGH_RISK_FRESHNESS = Duration.ofHours(24);
    private static final DateTimeFormatter MEASURE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final HealthProfileMapper profileMapper;
    private final HealthMetricMapper metricMapper;
    private final MedicalRecordMapper medicalRecordMapper;

    public HealthContext load(Long userId, boolean authorized) {
        if (!authorized) return HealthContext.empty();
        List<String> categories = new ArrayList<>();
        StringBuilder summary = new StringBuilder();
        boolean highRisk = false;

        HealthProfile profile = profileMapper.selectOne(Wrappers.<HealthProfile>lambdaQuery()
                .eq(HealthProfile::getUserId, userId).last("LIMIT 1"));
        if (profile != null) {
            List<String> facts = new ArrayList<>();
            if (StringUtils.hasText(profile.getAllergyHistory())) facts.add("过敏史：" + profile.getAllergyHistory());
            if (StringUtils.hasText(profile.getPastHistory())) facts.add("既往史：" + profile.getPastHistory());
            if (StringUtils.hasText(profile.getFamilyHistory())) facts.add("家族史：" + profile.getFamilyHistory());
            if (!facts.isEmpty()) { categories.add("健康档案"); summary.append(String.join("；", facts)).append('\n'); }
        }

        List<HealthMetric> metrics = metricMapper.selectList(Wrappers.<HealthMetric>lambdaQuery()
                .eq(HealthMetric::getUserId, userId)
                .ge(HealthMetric::getMeasureTime, LocalDateTime.now().minusDays(30))
                .orderByDesc(HealthMetric::getMeasureTime).last("LIMIT 10"));
        if (!metrics.isEmpty()) {
            categories.add("近30天体征");
            LocalDateTime now = LocalDateTime.now();
            boolean hasAbnormalMetric = false;
            for (HealthMetric metric : metrics) {
                MetricRules.Judge judge = MetricRules.judge(metric);
                if (judge.abnormal()) {
                    if (!hasAbnormalMetric) {
                        summary.append("历史体征记录（均为对应测量时点的数据，不能据此判断用户当前仍处于该状态）：\n");
                        hasAbnormalMetric = true;
                    }
                    summary.append("- 测量时间：").append(formatMeasureTime(metric.getMeasureTime(), now))
                            .append("；记录结果：").append(judge.message()).append('\n');
                    if ("HIGH".equals(judge.level()) && isFresh(metric.getMeasureTime(), now)) highRisk = true;
                }
            }
            if (hasAbnormalMetric) summary.append("回答时必须使用“历史记录显示”并注明测量时间；除非用户另行确认，不得称为“目前/当前”数值。\n");
        }

        List<MedicalRecord> records = medicalRecordMapper.selectList(Wrappers.<MedicalRecord>lambdaQuery()
                .eq(MedicalRecord::getUserId, userId).orderByDesc(MedicalRecord::getVisitDate).last("LIMIT 3"));
        if (!records.isEmpty()) {
            categories.add("近期就诊记录");
            for (MedicalRecord record : records) {
                if (StringUtils.hasText(record.getDiagnosis())) summary.append("既往就诊记录：").append(record.getDiagnosis()).append('；');
            }
        }
        if (summary.isEmpty()) summary.append("用户已授权，但当前没有可用的健康档案内容。");
        return new HealthContext(summary.toString().trim(), List.copyOf(categories), highRisk);
    }

    private String formatMeasureTime(LocalDateTime measureTime, LocalDateTime now) {
        if (measureTime == null) return "未知";
        Duration age = Duration.between(measureTime, now);
        String relative;
        if (age.isNegative()) relative = "时间晚于系统当前时间";
        else if (age.toHours() < 1) relative = "约" + Math.max(0, age.toMinutes()) + "分钟前";
        else if (age.toHours() < 24) relative = "约" + age.toHours() + "小时前";
        else relative = "约" + age.toDays() + "天前";
        return measureTime.format(MEASURE_TIME_FORMAT) + "（" + relative + "）";
    }

    private boolean isFresh(LocalDateTime measureTime, LocalDateTime now) {
        if (measureTime == null || measureTime.isAfter(now)) return false;
        return Duration.between(measureTime, now).compareTo(HIGH_RISK_FRESHNESS) <= 0;
    }
}
