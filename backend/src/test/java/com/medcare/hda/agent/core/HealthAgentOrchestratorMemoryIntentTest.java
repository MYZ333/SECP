package com.medcare.hda.agent.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HealthAgentOrchestratorMemoryIntentTest {

    @Test
    void recognizesExplicitCrossSessionMemoryQuestion() {
        assertTrue(HealthAgentOrchestrator.isMemoryRecallIntent("你还记得我来自哪只篮球队吗？"));
        assertTrue(HealthAgentOrchestrator.isMemoryRecallIntent("我之前说过自己打什么位置？"));
    }

    @Test
    void doesNotTreatOrdinaryHealthQuestionAsMemoryRecall() {
        assertFalse(HealthAgentOrchestrator.isMemoryRecallIntent("篮球运动员应该怎么保护膝盖？"));
        assertFalse(HealthAgentOrchestrator.isMemoryRecallIntent("我最近膝盖疼怎么办？"));
    }
}
