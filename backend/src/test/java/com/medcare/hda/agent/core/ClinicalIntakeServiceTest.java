package com.medcare.hda.agent.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.mockito.Answers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    void shouldKeepConfirmedFactsWhenModelOnlyReturnsCurrentRoundFacts() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec request = mock(ChatClient.ChatClientRequestSpec.class, Answers.RETURNS_SELF);
        ChatClient.CallResponseSpec response = mock(ChatClient.CallResponseSpec.class);
        when(chatClient.prompt()).thenReturn(request);
        when(request.call()).thenReturn(response);
        when(response.content()).thenReturn("""
                {"decision":"ASK","clinicalSummary":"偶尔腹泻或呕吐",
                 "knownFacts":["偶尔（每天3次以下）"],"missingFields":["腹痛部位"],
                 "question":{"prompt":"腹痛主要集中在哪个部位？","options":["上腹部","下腹部"]},
                 "newEpisode":false,"insufficient":false}
                """);
        ClinicalIntakeService modelService = new ClinicalIntakeService(
                chatClient, new ObjectMapper(), true, "sk-valid", 6);
        ClinicalIntakeState state = new ClinicalIntakeState(1L, "session", "episode", "COLLECTING", 4,
                "我肠胃炎犯了", "已确认腹痛并伴有腹泻或呕吐",
                List.of("腹痛或腹部绞痛", "伴有腹泻或呕吐"), List.of("频率", "腹痛部位"));

        ClinicalIntakeAssessment result = modelService.assess("偶尔（每天3次以下）", state, HealthContext.empty());

        assertTrue(result.knownFacts().contains("腹痛或腹部绞痛"));
        assertTrue(result.knownFacts().contains("伴有腹泻或呕吐"));
        assertTrue(result.knownFacts().contains("偶尔（每天3次以下）"));
        assertTrue(result.question().prompt().startsWith("腹痛主要"));
    }
}
