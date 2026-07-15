package com.medcare.hda.service;

import com.medcare.hda.dto.AlertActionDTO;
import com.medcare.hda.entity.HealthAlert;
import com.medcare.hda.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HealthAlertLifecycleServiceTest {
    private HealthAlertService alertService;
    private HealthAlertLifecycleService lifecycle;
    private HealthAlert alert;

    @BeforeEach
    void setUp() {
        alertService = mock(HealthAlertService.class);
        lifecycle = new HealthAlertLifecycleService(alertService);
        alert = new HealthAlert();
        alert.setId(8L);
        alert.setUserId(3L);
        alert.setStatus("OPEN");
        alert.setReadFlag(0);
        when(alertService.getById(8L)).thenReturn(alert);
    }

    @Test
    void shouldMoveFromOpenToInProgressAndResolved() {
        AlertActionDTO handling = new AlertActionDTO();
        handling.setChannel("HEALTH_ASSISTANT");
        handling.setSessionId("session-1");
        lifecycle.startHandling(8L, 3L, handling);
        assertEquals("IN_PROGRESS", alert.getStatus());
        assertEquals("session-1", alert.getRelatedSessionId());

        AlertActionDTO resolution = new AlertActionDTO();
        resolution.setNote("复测恢复正常");
        lifecycle.resolve(8L, 3L, resolution);
        assertEquals("RESOLVED", alert.getStatus());
        assertEquals("复测恢复正常", alert.getResolutionNote());
        assertNotNull(alert.getResolvedTime());
        verify(alertService, org.mockito.Mockito.times(2)).updateById(alert);
    }

    @Test
    void shouldRejectCrossUserAccess() {
        assertThrows(BusinessException.class, () -> lifecycle.acknowledge(8L, 4L));
    }
}
