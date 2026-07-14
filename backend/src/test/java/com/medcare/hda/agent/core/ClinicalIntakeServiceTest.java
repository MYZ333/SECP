package com.medcare.hda.agent.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ClinicalIntakeServiceTest {
    private final ClinicalIntakeService service = new ClinicalIntakeService(
            mock(ChatClient.class), new ObjectMapper(), true, "sk-placeholder", 6);

    @Test
    void shouldAskOneQuestionWithOptionsForVaguePersonalSymptom() {
        ClinicalIntakeAssessment result = service.assess("肚子疼怎么办", null, HealthContext.empty());

        assertEquals(ClinicalIntakeAssessment.Decision.ASK, result.decision());
        assertNotNull(result.question());
        assertTrue(result.question().options().size() >= 2);
        assertTrue(result.missingFields().contains("起病与持续时间"));
    }

    @Test
    void shouldAdvanceToNextMissingFieldAfterEachAnswer() {
        ClinicalIntakeState state = new ClinicalIntakeState(1L, "session", "episode", "COLLECTING", 1,
                "肚子疼", "肚子疼\n待回答问题：持续多久了", List.of("肚子疼"),
                List.of("起病与持续时间", "部位、严重程度与变化趋势", "伴随症状和危险信号"));

        ClinicalIntakeAssessment result = service.assess("2—7天", state, HealthContext.empty());

        assertEquals(ClinicalIntakeAssessment.Decision.ASK, result.decision());
        assertNotNull(result.question());
        assertTrue(result.question().prompt().contains("程度"));
        assertEquals("部位、严重程度与变化趋势", result.missingFields().getFirst());
    }

    @Test
    void shouldSendGeneralEducationDirectlyToAnswerFlow() {
        ClinicalIntakeAssessment result = service.assess("什么是高血压", null, HealthContext.empty());

        assertEquals(ClinicalIntakeAssessment.Decision.DIRECT_EDUCATION, result.decision());
        assertNull(result.question());
    }

    @Test
    void shouldStopAskingWhenUserSkipsOrRoundLimitIsReached() {
        ClinicalIntakeState state = new ClinicalIntakeState(1L, "session", "episode", "COLLECTING", 2,
                "肚子疼", "肚子疼，持续时间不详", List.of("肚子疼"), List.of("持续时间"));

        ClinicalIntakeAssessment result = service.assess("不知道，先按现有信息回答", state, HealthContext.empty());

        assertEquals(ClinicalIntakeAssessment.Decision.READY, result.decision());
        assertTrue(result.insufficient());
        assertTrue(result.clinicalSummary().contains("肚子疼"));
    }
}
