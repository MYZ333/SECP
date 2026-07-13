package com.medcare.hda.agent.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HealthAssistantAgentConfigTest {

    @Test
    void shouldRejectMissingBaiLianApiKey() {
        assertThrows(IllegalStateException.class, () -> new HealthAssistantAgentConfig(""));
        assertDoesNotThrow(() -> new HealthAssistantAgentConfig("sk-test"));
    }
}
