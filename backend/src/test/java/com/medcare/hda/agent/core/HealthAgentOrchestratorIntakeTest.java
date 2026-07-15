package com.medcare.hda.agent.core;

import com.medcare.hda.agent.api.AgentIntakeQuestion;
import com.medcare.hda.agent.knowledge.KnowledgeRetrievalService;
import com.medcare.hda.agent.doctor.DoctorRecommendationTool;
import com.medcare.hda.agent.repository.AgentAuditRepository;
import com.medcare.hda.agent.repository.ClinicalIntakeStateRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        assertFalse(result.directContent().contains("可选答案"));
        assertFalse(result.directContent().contains("自由填写"));
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

    @Test
    void shouldPreserveDoctorRecommendationIntentAcrossIntakeRounds() {
        Fixture fixture = fixture();
        stubConsultation(fixture);
        ClinicalIntakeState state = new ClinicalIntakeState(1L, "session", "episode", "COLLECTING", 6,
                "给我推荐一个医生，我最近肠胃炎犯了", "今日水样腹泻1-3次，无脓血",
                List.of("腹泻", "今日开始", "水样便"), List.of());
        when(fixture.stateRepository.findActive(1L, "session")).thenReturn(Optional.of(state));
        when(fixture.intakeService.assess(anyString(), any(), any())).thenReturn(new ClinicalIntakeAssessment(
                ClinicalIntakeAssessment.Decision.READY, state.clinicalSummary(), state.knownFacts(),
                List.of(), null, false, false));

        PreparedAgentResponse result = fixture.orchestrator.prepare(
                1L, new AgentConversation("session", "conversation"), "没有，只是单纯的水样便", false);

        assertEquals("CONSULTATION+EVIDENCE+DOCTOR_TOOL", result.route());
        verify(fixture.doctorRecommendationTool).recommend("没有，只是单纯的水样便", state.clinicalSummary());
    }

    @Test
    void shouldUseRecentStructuredSummaryWhenUserAcceptsLatestDoctorOffer() {
        Fixture fixture = fixture();
        stubConsultation(fixture);
        ClinicalIntakeState completed = new ClinicalIntakeState(1L, "session", "episode", "COMPLETED", 4,
                "我今天腹泻", "今日水样腹泻1-3次，无脓血", List.of("腹泻", "水样便"), List.of());
        when(fixture.auditRepository.wasDoctorRecommendationOffered(1L, "session")).thenReturn(true);
        when(fixture.stateRepository.findRecentCompleted(1L, "session")).thenReturn(Optional.of(completed));

        PreparedAgentResponse result = fixture.orchestrator.prepare(
                1L, new AgentConversation("session", "conversation"), "需要", false);

        assertEquals("CONSULTATION+EVIDENCE+DOCTOR_TOOL", result.route());
        verify(fixture.intakeService, never()).assess(anyString(), any(), any());
        verify(fixture.doctorRecommendationTool).recommend("需要", completed.clinicalSummary());
    }

    private void stubConsultation(Fixture fixture) {
        ChatClient.ChatClientRequestSpec request = mock(ChatClient.ChatClientRequestSpec.class, Answers.RETURNS_SELF);
        ChatClient.CallResponseSpec response = mock(ChatClient.CallResponseSpec.class);
        when(fixture.chatClient.prompt()).thenReturn(request);
        when(request.call()).thenReturn(response);
        when(response.content()).thenReturn("保守健康咨询摘要");
        when(fixture.retrievalService.search(anyString())).thenReturn(List.of());
        when(fixture.doctorRecommendationTool.recommend(anyString(), anyString())).thenReturn(List.of());
    }

    private Fixture fixture() {
        HealthContextService contextService = mock(HealthContextService.class);
        when(contextService.load(anyLong(), anyBoolean())).thenReturn(HealthContext.empty());
        ClinicalIntakeService intakeService = mock(ClinicalIntakeService.class);
        ClinicalIntakeStateRepository stateRepository = mock(ClinicalIntakeStateRepository.class);
        when(stateRepository.findActive(anyLong(), anyString())).thenReturn(Optional.empty());
        KnowledgeRetrievalService retrievalService = mock(KnowledgeRetrievalService.class);
        AgentAuditRepository auditRepository = mock(AgentAuditRepository.class);
        DoctorRecommendationTool doctorRecommendationTool = mock(DoctorRecommendationTool.class);
        ChatClient chatClient = mock(ChatClient.class);
        Executor executor = Runnable::run;
        HealthAgentOrchestrator orchestrator = new HealthAgentOrchestrator(contextService, new SafetyTriageService(),
                intakeService, stateRepository, retrievalService, auditRepository, doctorRecommendationTool,
                chatClient, executor, 20, true);
        return new Fixture(orchestrator, intakeService, stateRepository, retrievalService, auditRepository,
                doctorRecommendationTool, chatClient);
    }

    private record Fixture(HealthAgentOrchestrator orchestrator, ClinicalIntakeService intakeService,
                           ClinicalIntakeStateRepository stateRepository,
                           KnowledgeRetrievalService retrievalService, AgentAuditRepository auditRepository,
                           DoctorRecommendationTool doctorRecommendationTool, ChatClient chatClient) { }
}
