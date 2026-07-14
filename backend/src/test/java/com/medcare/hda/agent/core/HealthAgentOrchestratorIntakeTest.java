package com.medcare.hda.agent.core;

import com.medcare.hda.agent.api.AgentIntakeQuestion;
import com.medcare.hda.agent.knowledge.KnowledgeRetrievalService;
import com.medcare.hda.agent.repository.AgentAuditRepository;
import com.medcare.hda.agent.repository.ClinicalIntakeStateRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HealthAgentOrchestratorIntakeTest {
    @Test
    void shouldReturnClarificationWithoutStartingConsultationOrRetrieval() {
        Fixture fixture = fixture();
        ClinicalIntakeAssessment assessment = new ClinicalIntakeAssessment(
                ClinicalIntakeAssessment.Decision.ASK, "肚子疼", List.of("肚子疼"),
                List.of("持续时间"), new AgentIntakeQuestion("什么时候开始的？",
                List.of("今天", "2—7天", "超过1周"), true), false, false);
        when(fixture.intakeService.assess(anyString(), any(), any())).thenReturn(assessment);

        PreparedAgentResponse result = fixture.orchestrator.prepare(
                1L, new AgentConversation("session", "conversation"), "肚子疼怎么办", false);

        assertEquals("CLARIFICATION", result.route());
        assertTrue(result.direct());
        assertTrue(result.directContent().contains("什么时候开始"));
        verify(fixture.stateRepository).saveClarification(anyLong(), anyString(), anyString(), any(), any());
        verify(fixture.retrievalService, never()).search(anyString());
        verify(fixture.chatClient, never()).prompt();
    }

    @Test
    void shouldDetectEmergencyAcrossIntakeRounds() {
        Fixture fixture = fixture();
        ClinicalIntakeState state = new ClinicalIntakeState(1L, "session", "episode", "COLLECTING", 1,
                "突然胸痛", "突然胸痛，持续时间不详", List.of("胸痛"), List.of("伴随症状"));
        when(fixture.stateRepository.findActive(1L, "session")).thenReturn(Optional.of(state));

        PreparedAgentResponse result = fixture.orchestrator.prepare(
                1L, new AgentConversation("session", "conversation"), "现在还冒冷汗", false);

        assertEquals("SAFETY_SHORT_CIRCUIT", result.route());
        assertEquals("EMERGENCY", result.risk().level());
        verify(fixture.intakeService, never()).assess(anyString(), any(), any());
        verify(fixture.retrievalService, never()).search(anyString());
    }

    @Test
    void shouldUseConsolidatedClinicalSummaryForRetrieval() {
        Fixture fixture = fixture();
        ChatClient.ChatClientRequestSpec request = mock(ChatClient.ChatClientRequestSpec.class, Answers.RETURNS_SELF);
        ChatClient.CallResponseSpec response = mock(ChatClient.CallResponseSpec.class);
        when(fixture.chatClient.prompt()).thenReturn(request);
        when(request.call()).thenReturn(response);
        when(response.content()).thenReturn("保守健康咨询摘要");
        String summary = "最初诉求：右下腹疼痛；补充信息：持续两天、疼痛加重、伴随低热";
        when(fixture.intakeService.assess(anyString(), any(), any())).thenReturn(new ClinicalIntakeAssessment(
                ClinicalIntakeAssessment.Decision.READY, summary, List.of("右下腹疼痛", "持续两天"),
                List.of(), null, false, false));
        when(fixture.retrievalService.search(summary)).thenReturn(List.of());

        PreparedAgentResponse result = fixture.orchestrator.prepare(
                1L, new AgentConversation("session", "conversation"), "持续两天了", false);

        assertEquals("CONSULTATION+EVIDENCE", result.route());
        verify(fixture.retrievalService).search(summary);
        verify(fixture.retrievalService, never()).search("持续两天了");
    }

    private Fixture fixture() {
        HealthContextService contextService = mock(HealthContextService.class);
        when(contextService.load(anyLong(), anyBoolean())).thenReturn(HealthContext.empty());
        ClinicalIntakeService intakeService = mock(ClinicalIntakeService.class);
        ClinicalIntakeStateRepository stateRepository = mock(ClinicalIntakeStateRepository.class);
        when(stateRepository.findActive(anyLong(), anyString())).thenReturn(Optional.empty());
        KnowledgeRetrievalService retrievalService = mock(KnowledgeRetrievalService.class);
        AgentAuditRepository auditRepository = mock(AgentAuditRepository.class);
        ChatClient chatClient = mock(ChatClient.class);
        Executor executor = Runnable::run;
        HealthAgentOrchestrator orchestrator = new HealthAgentOrchestrator(contextService, new SafetyTriageService(),
                intakeService, stateRepository, retrievalService, auditRepository, chatClient, executor, 20);
        return new Fixture(orchestrator, intakeService, stateRepository, retrievalService, chatClient);
    }

    private record Fixture(HealthAgentOrchestrator orchestrator, ClinicalIntakeService intakeService,
                           ClinicalIntakeStateRepository stateRepository,
                           KnowledgeRetrievalService retrievalService, ChatClient chatClient) { }
}
