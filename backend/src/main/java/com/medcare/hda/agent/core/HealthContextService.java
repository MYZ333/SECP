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
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HealthContextService {
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
            for (HealthMetric metric : metrics) {
                MetricRules.Judge judge = MetricRules.judge(metric);
                if (judge.abnormal()) {
                    summary.append(judge.message()).append('；');
                    if ("HIGH".equals(judge.level())) highRisk = true;
                }
            }
            summary.append('\n');
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
}
