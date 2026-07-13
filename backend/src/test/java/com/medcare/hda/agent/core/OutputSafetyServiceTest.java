package com.medcare.hda.agent.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutputSafetyServiceTest {
    @Test
    void shouldRemoveDefinitiveDiagnosisAndAppendDisclaimer() {
        String result = new OutputSafetyService().enforce("您患有高血压。", new RiskAssessment("LOW", "", false));
        assertFalse(result.contains("您患有"));
        assertTrue(result.contains("不能替代"));
    }

    @Test
    void highRiskMustRecommendMedicalCare() {
        String result = new OutputSafetyService().enforce("请注意休息。", new RiskAssessment("HIGH", "", false));
        assertTrue(result.contains("就医"));
    }
}
