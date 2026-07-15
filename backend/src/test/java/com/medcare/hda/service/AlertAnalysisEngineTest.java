package com.medcare.hda.service;

import com.medcare.hda.dto.AlertAnalysisItemVO;
import com.medcare.hda.dto.AlertAnalysisVO;
import com.medcare.hda.entity.HealthMetric;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlertAnalysisEngineTest {
    private final AlertAnalysisEngine engine = new AlertAnalysisEngine();

    @Test
    void shouldAggregateByTypeAndKeepLatestAndHighestRiskMessages() {
        LocalDateTime end = LocalDateTime.of(2026, 7, 15, 12, 0);
        HealthMetric olderHigh = metric(1L, "BLOOD_PRESSURE", 170, 105D, end.minusDays(2));
        HealthMetric latestMedium = metric(2L, "BLOOD_PRESSURE", 145, 92D, end.minusHours(2));
        HealthMetric normalTemperature = metric(3L, "TEMPERATURE", 36.8, null, end.minusHours(1));
        HealthMetric unsupportedWeight = metric(4L, "WEIGHT", 90, null, end.minusHours(1));

        AlertAnalysisVO result = engine.analyze(9L,
                List.of(olderHigh, latestMedium, normalTemperature, unsupportedWeight), end.minusDays(7), end);

        assertEquals(4, result.getMetricCount());
        assertEquals(3, result.getAnalyzableMetricCount());
        assertEquals(2, result.getAbnormalCount());
        assertEquals(1, result.getHighRiskCount());
        AlertAnalysisItemVO item = result.getAbnormalTypes().getFirst();
        assertEquals("HIGH", item.getLevel());
        assertEquals(2, item.getAbnormalCount());
        assertTrue(item.getLatestMessage().contains("145/92"));
        assertTrue(item.getHighestRiskMessage().contains("170/105"));
    }

    @Test
    void fingerprintShouldChangeWhenMetricChanges() {
        LocalDateTime end = LocalDateTime.of(2026, 7, 15, 12, 0);
        HealthMetric first = metric(1L, "BLOOD_SUGAR", 8.0, null, end.minusHours(1));
        String key1 = engine.analyze(9L, List.of(first), end.minusDays(7), end)
                .getAbnormalTypes().getFirst().getGenerationKey();
        first.setMetricValue(9.0);
        String key2 = engine.analyze(9L, List.of(first), end.minusDays(7), end)
                .getAbnormalTypes().getFirst().getGenerationKey();
        assertNotEquals(key1, key2);
    }

    private HealthMetric metric(Long id, String type, double value, Double value2, LocalDateTime time) {
        HealthMetric metric = new HealthMetric();
        metric.setId(id);
        metric.setMetricType(type);
        metric.setMetricValue(value);
        metric.setMetricValue2(value2);
        metric.setMeasureTime(time);
        metric.setUpdateTime(time);
        return metric;
    }
}
