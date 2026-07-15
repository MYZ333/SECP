package com.medcare.hda.common;

import com.medcare.hda.entity.HealthMetric;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetricRulesTest {

    @Test
    void shouldClassifyBoundaryValues() {
        assertEquals("MEDIUM", judge("BLOOD_PRESSURE", 140, 80D).level());
        assertEquals("HIGH", judge("BLOOD_PRESSURE", 160, 80D).level());
        assertEquals("MEDIUM", judge("BLOOD_SUGAR", 7.0, null).level());
        assertEquals("HIGH", judge("BLOOD_SUGAR", 3.8, null).level());
        assertEquals("MEDIUM", judge("HEART_RATE", 101, null).level());
        assertEquals("HIGH", judge("HEART_RATE", 121, null).level());
        assertEquals("MEDIUM", judge("TEMPERATURE", 37.3, null).level());
        assertEquals("HIGH", judge("TEMPERATURE", 39.0, null).level());
    }

    @Test
    void shouldIgnoreUnsupportedMetrics() {
        assertFalse(judge("WEIGHT", 95, null).abnormal());
        assertTrue(MetricRules.supports("BLOOD_PRESSURE"));
        assertFalse(MetricRules.supports("WEIGHT"));
    }

    @Test
    void shouldRejectClearlyInvalidInputBeforeRiskJudgement() {
        HealthMetric sugar = new HealthMetric();
        sugar.setMetricType("BLOOD_SUGAR");
        sugar.setMetricValue(11111D);
        assertTrue(MetricRules.validationMessage(sugar).contains("超出常见测量范围"));

        HealthMetric pressure = new HealthMetric();
        pressure.setMetricType("BLOOD_PRESSURE");
        pressure.setMetricValue(120D);
        assertEquals("请输入舒张压", MetricRules.validationMessage(pressure));

        sugar.setMetricValue(8.2D);
        assertEquals(null, MetricRules.validationMessage(sugar));
    }

    private MetricRules.Judge judge(String type, double value, Double value2) {
        HealthMetric metric = new HealthMetric();
        metric.setMetricType(type);
        metric.setMetricValue(value);
        metric.setMetricValue2(value2);
        return MetricRules.judge(metric);
    }
}
