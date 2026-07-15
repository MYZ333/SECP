package com.medcare.hda.agent.core;

import com.medcare.hda.agent.api.AgentStreamEvent;
import com.medcare.hda.agent.api.AgentStageUpdate;
import com.medcare.hda.agent.api.AgentIntakeQuestion;
import com.medcare.hda.agent.api.DoctorRecommendation;
import com.medcare.hda.agent.api.DoctorRecommendationAction;
import com.medcare.hda.agent.repository.AgentAuditRepository;
import com.medcare.hda.agent.repository.AgentConversationRepository;
import com.medcare.hda.agent.repository.ClinicalIntakeStateRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.function.Consumer;

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

class HealthAssistantAgentServiceStreamingTest {

    @Test
    void shouldUseNativeChatClientStreamAndFinishWithDone() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec request = mock(ChatClient.ChatClientRequestSpec.class, Answers.RETURNS_SELF);
        ChatClient.StreamResponseSpec streamResponse = mock(ChatClient.StreamResponseSpec.class);
        when(chatClient.prompt()).thenReturn(request);
        when(request.stream()).thenReturn(streamResponse);
        when(streamResponse.content()).thenReturn(Flux.just(
                "第一段模型增量内容，用来验证系统不会等待完整回答以后再按固定字符切片。",
                "第二段模型增量内容，会继续经过安全窗口并通过SSE发送给浏览器。"));

        ChatMemory memory = mock(ChatMemory.class);
        when(memory.get(anyString())).thenReturn(List.of());
        AgentConversationRepository conversations = mock(AgentConversationRepository.class);
        HealthAgentOrchestrator orchestrator = mock(HealthAgentOrchestrator.class);
        AgentAuditRepository audit = mock(AgentAuditRepository.class);
        ClinicalIntakeStateRepository intakeState = mock(ClinicalIntakeStateRepository.class);
        DoctorRecommendation recommendation = new DoctorRecommendation(1L, "李医生", null, "主任医师",
                "市第一人民医院", "心血管内科", "高血压", "", 96,
                List.of("科室与当前健康诉求匹配"),
                new DoctorRecommendationAction("START_DOCTOR_CONSULT", "/doctor-consult", 1L));
        when(orchestrator.prepare(anyLong(), any(), anyString(), anyBoolean(), any(Consumer.class))).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Consumer<AgentStageUpdate> reporter = invocation.getArgument(4);
            reporter.accept(AgentStageUpdate.completed("SAFETY_CHECK", "未识别到紧急风险信号"));
            reporter.accept(AgentStageUpdate.running("SYNTHESIZING", "正在整合回答"));
            return new PreparedAgentResponse("trace-stream", new RiskAssessment("LOW", "一般咨询", false),
                    List.of(), List.of(), List.of(), List.of(recommendation), "CONSULTATION+DOCTOR_TOOL",
                    "system prompt", null, null);
        });

        HealthAssistantAgentService service = new HealthAssistantAgentService(chatClient, memory, conversations,
                orchestrator, new OutputSafetyService(), audit, intakeState);
        List<AgentStreamEvent> events = service.stream(2L, new AgentConversation("session", "conversation"),
                "测试流式回答", false).collectList().block();

        verify(request).stream();
        verify(request, never()).call();
        assertTrue(events.stream().anyMatch(event -> "delta".equals(event.type())));
        assertEquals("done", events.getLast().type());
        String answer = events.stream().filter(event -> "delta".equals(event.type()))
                .map(AgentStreamEvent::content).reduce("", String::concat);
        assertTrue(answer.contains("第一段模型增量"));
        assertTrue(answer.contains("第二段模型增量"));
        assertTrue(answer.contains("不能替代"));
        assertTrue(events.stream().anyMatch(event -> "doctor_recommendations".equals(event.type())
                && event.recommendedDoctors().size() == 1));
        assertTrue(indexOf(events, "doctor_recommendations", null) < indexOf(events, "done", null));
        verify(intakeState).complete(2L, "session");
        int safetyIndex = indexOf(events, "stage", "SAFETY_CHECK");
        int synthesisIndex = indexOf(events, "stage", "SYNTHESIZING");
        int firstDeltaIndex = indexOf(events, "delta", null);
        assertTrue(safetyIndex >= 0 && safetyIndex < firstDeltaIndex);
        assertTrue(synthesisIndex >= 0 && synthesisIndex < firstDeltaIndex);
    }

    @Test
    void shouldOnlyExposeSafetyProgressForEmergencyShortCircuit() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatMemory memory = mock(ChatMemory.class);
        AgentConversationRepository conversations = mock(AgentConversationRepository.class);
        HealthAgentOrchestrator orchestrator = mock(HealthAgentOrchestrator.class);
        AgentAuditRepository audit = mock(AgentAuditRepository.class);
        ClinicalIntakeStateRepository intakeState = mock(ClinicalIntakeStateRepository.class);
        when(orchestrator.prepare(anyLong(), any(), anyString(), anyBoolean(), any(Consumer.class))).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Consumer<AgentStageUpdate> reporter = invocation.getArgument(4);
            reporter.accept(AgentStageUpdate.completed("SAFETY_CHECK", "发现需要立即处理的危险信号"));
            return new PreparedAgentResponse("trace-emergency", new RiskAssessment("EMERGENCY", "需要立即急诊评估", true),
                    List.of(), List.of(), List.of(), List.of(), "SAFETY_SHORT_CIRCUIT", null, "请立即联系急救服务。", null);
        });

        HealthAssistantAgentService service = new HealthAssistantAgentService(chatClient, memory, conversations,
                orchestrator, new OutputSafetyService(), audit, intakeState);
        List<AgentStreamEvent> events = service.stream(2L, new AgentConversation("session", "conversation"),
                "突发严重胸痛", false).collectList().block();

        assertEquals("done", events.getLast().type());
        assertTrue(events.stream().anyMatch(event -> "SAFETY_CHECK".equals(event.stage())));
        assertTrue(events.stream().noneMatch(event -> "SYNTHESIZING".equals(event.stage())));
        verify(intakeState).complete(2L, "session");
        verify(chatClient, never()).prompt();
    }

    @Test
    void shouldPersistClarificationWithoutCompletingIntakeEpisode() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatMemory memory = mock(ChatMemory.class);
        AgentConversationRepository conversations = mock(AgentConversationRepository.class);
        HealthAgentOrchestrator orchestrator = mock(HealthAgentOrchestrator.class);
        AgentAuditRepository audit = mock(AgentAuditRepository.class);
        ClinicalIntakeStateRepository intakeState = mock(ClinicalIntakeStateRepository.class);
        when(orchestrator.prepare(anyLong(), any(), anyString(), anyBoolean(), any(Consumer.class))).thenReturn(
                new PreparedAgentResponse("trace-clarify", new RiskAssessment("LOW", "一般咨询", false),
                        List.of(), List.of(), List.of(), List.of(), "CLARIFICATION", null,
                        "请补充症状持续时间。", new AgentIntakeQuestion("持续多久了？",
                        List.of("今天", "2—7天", "超过1周"), true)));
        HealthAssistantAgentService service = new HealthAssistantAgentService(chatClient, memory, conversations,
                orchestrator, new OutputSafetyService(), audit, intakeState);

        List<AgentStreamEvent> events = service.stream(2L, new AgentConversation("session", "conversation"),
                "肚子疼", false).collectList().block();

        assertEquals("done", events.getLast().type());
        assertTrue(events.stream().filter(event -> "delta".equals(event.type()))
                .map(AgentStreamEvent::content).reduce("", String::concat).contains("请补充"));
        assertTrue(events.stream().anyMatch(event -> "intake".equals(event.type())
                && event.intakeQuestion() != null));
        verify(intakeState, never()).complete(anyLong(), anyString());
        verify(chatClient, never()).prompt();
    }

    @Test
    void shouldOfferRealDoctorMatchingAfterOrdinaryConsultation() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec request = mock(ChatClient.ChatClientRequestSpec.class, Answers.RETURNS_SELF);
        ChatClient.StreamResponseSpec streamResponse = mock(ChatClient.StreamResponseSpec.class);
        when(chatClient.prompt()).thenReturn(request);
        when(request.stream()).thenReturn(streamResponse);
        when(streamResponse.content()).thenReturn(Flux.just("可以先补液并继续观察症状。"));
        ChatMemory memory = mock(ChatMemory.class);
        when(memory.get(anyString())).thenReturn(List.of());
        AgentConversationRepository conversations = mock(AgentConversationRepository.class);
        HealthAgentOrchestrator orchestrator = mock(HealthAgentOrchestrator.class);
        AgentAuditRepository audit = mock(AgentAuditRepository.class);
        ClinicalIntakeStateRepository intakeState = mock(ClinicalIntakeStateRepository.class);
        when(orchestrator.prepare(anyLong(), any(), anyString(), anyBoolean(), any(Consumer.class))).thenReturn(
                new PreparedAgentResponse("trace-offer", new RiskAssessment("LOW", "一般咨询", false),
                        List.of(), List.of(), List.of(), List.of(), "CONSULTATION+EVIDENCE",
                        "system prompt", null, null));
        HealthAssistantAgentService service = new HealthAssistantAgentService(chatClient, memory, conversations,
                orchestrator, new OutputSafetyService(), audit, intakeState);

        List<AgentStreamEvent> events = service.stream(2L, new AgentConversation("session", "conversation"),
                "今天腹泻两次", false).collectList().block();
        String answer = events.stream().filter(event -> "delta".equals(event.type()))
                .map(AgentStreamEvent::content).reduce("", String::concat);

        assertTrue(answer.contains("是否需要我根据本次问诊信息"));
        assertTrue(answer.contains("匹配平台内已审核的真实医生"));
        assertTrue(events.stream().noneMatch(event -> "doctor_recommendations".equals(event.type())));
    }

    private int indexOf(List<AgentStreamEvent> events, String type, String stage) {
        for (int index = 0; index < events.size(); index++) {
            AgentStreamEvent event = events.get(index);
            if (type.equals(event.type()) && (stage == null || stage.equals(event.stage()))) return index;
        }
        return -1;
    }
}
