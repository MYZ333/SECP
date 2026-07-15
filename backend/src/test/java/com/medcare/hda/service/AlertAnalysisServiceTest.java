package com.medcare.hda.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.medcare.hda.dto.AlertAnalysisVO;
import com.medcare.hda.entity.HealthAlert;
import com.medcare.hda.entity.HealthMetric;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlertAnalysisServiceTest {
    private HealthAlertService alertService;
    private HealthMetricService metricService;
    private AlertAnalysisService service;

    @BeforeEach
    void setUp() {
        alertService = mock(HealthAlertService.class);
        metricService = mock(HealthMetricService.class);
        service = new AlertAnalysisService(alertService, metricService, new AlertAnalysisEngine());
        HealthMetric metric = new HealthMetric();
        metric.setId(1L);
        metric.setMetricType("BLOOD_SUGAR");
        metric.setMetricValue(12.0);
        metric.setMeasureTime(LocalDateTime.now().minusHours(1));
        metric.setUpdateTime(metric.getMeasureTime());
        when(metricService.list(any(Wrapper.class))).thenReturn(List.of(metric));
    }

    @Test
    void shouldSaveNewAnalysisResult() {
        when(alertService.count(any(Wrapper.class))).thenReturn(0L);
        when(alertService.save(any(HealthAlert.class))).thenReturn(true);

        AlertAnalysisVO result = service.generate(7L);

        assertEquals(1, result.getGeneratedCount());
        assertEquals(0, result.getDuplicateCount());
        verify(alertService).save(any(HealthAlert.class));
    }

    @Test
    void shouldSkipPreviouslyGeneratedResult() {
        when(alertService.count(any(Wrapper.class))).thenReturn(1L);

        AlertAnalysisVO result = service.generate(7L);

        assertEquals(0, result.getGeneratedCount());
        assertEquals(1, result.getDuplicateCount());
        verify(alertService, never()).save(any(HealthAlert.class));
    }

    @Test
    void shouldUpdateExistingActiveAlertInsteadOfCreatingAnotherRow() {
        HealthAlert existing = new HealthAlert();
        existing.setId(99L);
        existing.setUserId(7L);
        existing.setAlertType("血糖异常");
        existing.setStatus("OPEN");
        existing.setTriggerCount(2);
        existing.setGenerationKey("old-key");
        when(alertService.getOne(any(Wrapper.class), org.mockito.ArgumentMatchers.eq(false))).thenReturn(existing);
        when(alertService.updateById(any(HealthAlert.class))).thenReturn(true);

        AlertAnalysisVO result = service.generate(7L);

        assertEquals(0, result.getGeneratedCount());
        assertEquals(1, result.getUpdatedCount());
        assertEquals(3, existing.getTriggerCount());
        verify(alertService).updateById(existing);
        verify(alertService, never()).save(any(HealthAlert.class));
    }
}
