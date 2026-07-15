package com.medcare.hda.agent.core;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.medcare.hda.entity.HealthMetric;
import com.medcare.hda.mapper.HealthMetricMapper;
import com.medcare.hda.mapper.HealthProfileMapper;
import com.medcare.hda.mapper.MedicalRecordMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HealthContextServiceTest {
    private final HealthProfileMapper profileMapper = mock(HealthProfileMapper.class);
    private final HealthMetricMapper metricMapper = mock(HealthMetricMapper.class);
    private final MedicalRecordMapper medicalRecordMapper = mock(MedicalRecordMapper.class);
    private final HealthContextService service = new HealthContextService(profileMapper, metricMapper, medicalRecordMapper);

    @Test
    @SuppressWarnings("unchecked")
    void shouldPreserveMeasurementTimeAndNotTreatTwoDayOldFeverAsCurrent() {
        LocalDateTime measuredAt = LocalDateTime.now().minusDays(2).minusMinutes(5);
        HealthMetric fever = temperature(39.0, measuredAt);
        when(profileMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(metricMapper.selectList(any(Wrapper.class))).thenReturn(List.of(fever));
        when(medicalRecordMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        HealthContext context = service.load(7L, true);

        assertTrue(context.summary().contains("历史体征记录"));
        assertTrue(context.summary().contains(measuredAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
        assertTrue(context.summary().contains("约2天前"));
        assertTrue(context.summary().contains("不能据此判断用户当前仍处于该状态"));
        assertTrue(context.summary().contains("不得称为“目前/当前”数值"));
        assertFalse(context.highRiskMetric());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldKeepFreshHighRiskMetricForTriageWithoutLosingTimestamp() {
        HealthMetric fever = temperature(39.0, LocalDateTime.now().minusHours(2));
        when(profileMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(metricMapper.selectList(any(Wrapper.class))).thenReturn(List.of(fever));
        when(medicalRecordMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        HealthContext context = service.load(7L, true);

        assertTrue(context.highRiskMetric());
        assertTrue(context.summary().contains("约2小时前"));
    }

    private HealthMetric temperature(double value, LocalDateTime measuredAt) {
        HealthMetric metric = new HealthMetric();
        metric.setUserId(7L);
        metric.setMetricType("TEMPERATURE");
        metric.setMetricValue(value);
        metric.setUnit("℃");
        metric.setMeasureTime(measuredAt);
        return metric;
    }
}
