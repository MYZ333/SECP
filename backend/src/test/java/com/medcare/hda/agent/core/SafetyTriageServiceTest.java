package com.medcare.hda.agent.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SafetyTriageServiceTest {
    private final SafetyTriageService service = new SafetyTriageService();

    @Test
    void shouldShortCircuitChestPainWithBreathingDifficulty() {
        RiskAssessment result = service.assess("突然胸痛、冒冷汗，还有呼吸困难", HealthContext.empty());
        assertEquals("EMERGENCY", result.level());
        assertTrue(result.emergency());
        assertTrue(result.message().contains("120"));
    }

    @Test
    void shouldRespectNegation() {
        RiskAssessment result = service.assess("我没有胸痛，也没有呼吸困难，只想了解心梗知识", HealthContext.empty());
        assertEquals("LOW", result.level());
        assertFalse(result.emergency());
    }

    @Test
    void shouldShortCircuitImminentDeathLanguage() {
        RiskAssessment result = service.assess("我要猝死了", HealthContext.empty());

        assertEquals("EMERGENCY", result.level());
        assertTrue(result.emergency());
        assertTrue(result.message().contains("120"));
    }
}
