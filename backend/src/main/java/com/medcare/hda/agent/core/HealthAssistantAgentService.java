package com.medcare.hda.agent.core;

import com.medcare.hda.agent.api.AgentChatResponse;
import com.medcare.hda.agent.api.AgentStageUpdate;
import com.medcare.hda.agent.api.AgentStreamEvent;
import com.medcare.hda.agent.repository.AgentAuditRepository;
import com.medcare.hda.agent.repository.AgentConversationRepository;
import com.medcare.hda.agent.repository.ClinicalIntakeStateRepository;
import com.medcare.hda.agent.memory.LongTermMemoryService;
import com.medcare.hda.agent.memory.MemorySourceAgent;
import com.medcare.hda.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthAssistantAgentService {
    private static final String DOCTOR_RECOMMENDATION_OFFER =
            "\n\n如果你希望进一步咨询，是否需要我根据本次问诊信息，为你匹配平台内已审核的真实医生？";
    public static final String SYSTEM_PROMPT = """
            你是一名面向普通用户的“健康信息与就医辅助助手”。你帮助用户理解健康问题、识别风险、准备就医沟通，但不能替代医生诊断、开具处方或决定治疗方案。
            必须遵守：
            1. 只把可靠资料支持的内容表述为事实；不确定内容使用“可能”“需要排除”等措辞。
            2. 不根据有限描述下确定诊断，不提供个体化处方、具体剂量调整、停药或换药指令。
            3. 遇到药物、孕产妇、儿童、高龄老人、慢病、多病共存或精神健康问题时采取保守策略。
            4. 紧急危险信号优先建议立即呼叫当地急救电话或前往急诊。
            5. 使用清晰、平实、非评判性的中文，不制造恐慌，也不淡化风险。
            6. 按“简短结论—现在可做什么—何时就医—就医沟通要点”组织回答。不要输出资料编号、资料依据或来源列表。
            7. 健康档案中的体征是带测量时间的历史记录。必须保留其时间语义；除非用户明确说这是当前测量值，否则不得改写成“目前/当前体温、血压、血糖”等现状。历史异常应先请用户复测或确认当前症状。
            """;

    private final ChatClient healthAssistantChatClient;
    private final ChatMemory healthAssistantChatMemory;
    private final AgentConversationRepository conversationRepository;
    private final HealthAgentOrchestrator orchestrator;
    private final OutputSafetyService outputSafetyService;
    private final AgentAuditRepository auditRepository;
    private final ClinicalIntakeStateRepository intakeStateRepository;

    @Autowired(required = false)
    private LongTermMemoryService longTermMemoryService;

    public AgentConversation prepareConversation(Long userId, String sessionId) {
        return conversationRepository.resolve(userId, sessionId);
    }

    public AgentChatResponse chat(Long userId, AgentConversation conversation, String message, boolean useHealthProfile) {
        Execution execution = execute(userId, conversation, message, useHealthProfile);
        PreparedAgentResponse prepared = execution.prepared();
        return new AgentChatResponse(conversation.sessionId(), execution.content(), "assistant",
                prepared.risk().level(), prepared.citations(), prepared.usedProfileCategories(), prepared.traceId(),
                prepared.intakeQuestion(), prepared.recommendedDoctors());
    }

    public Flux<AgentStreamEvent> stream(Long userId, AgentConversation conversation, String message, boolean useHealthProfile) {
        long start = System.currentTimeMillis();
        return Flux.create(sink -> {
            sink.next(AgentStreamEvent.meta(conversation.sessionId()));
            sink.next(AgentStreamEvent.stage(AgentStageUpdate.running("SAFETY_CHECK", "正在检查紧急风险信号")));

            Mono.fromCallable(() -> orchestrator.prepare(userId, conversation, message, useHealthProfile,
                            stage -> sink.next(AgentStreamEvent.stage(stage))))
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMapMany(prepared -> streamPrepared(userId, conversation, message, prepared, start))
                    .subscribe(sink::next,
                            error -> sink.error(error instanceof BusinessException ? error : new BusinessException(friendlyMessage(error))),
                            sink::complete);
        });
    }

    private Flux<AgentStreamEvent> streamPrepared(Long userId, AgentConversation conversation, String message,
                                                   PreparedAgentResponse prepared, long start) {
        Flux<AgentStreamEvent> header = Flux.fromIterable(finalizationEvents(conversation, prepared));
        if (prepared.direct()) {
            String content = addDoctorRecommendationOffer(
                    enforceOutput(prepared.directContent(), prepared), prepared);
            return Flux.concat(header, Flux.fromIterable(textEvents(content)),
                    recommendationEvents(prepared),
                    persistAndDone(userId, conversation, message, prepared, content, start));
        }

        StreamingSafetyWindow safetyWindow = new StreamingSafetyWindow(outputSafetyService, prepared.risk(),
                shouldOfferDoctorRecommendation(prepared));
        Flux<AgentStreamEvent> modelDeltas = healthAssistantChatClient.prompt()
                .system(withLongTermMemory(userId, message, prepared.systemPrompt()))
                .messages(healthAssistantChatMemory.get(conversation.conversationId()))
                .user(message)
                .stream()
                .content()
                .filter(token -> token != null && !token.isEmpty())
                .map(safetyWindow::accept)
                .filter(token -> !token.isEmpty())
                .map(AgentStreamEvent::delta);

        Flux<AgentStreamEvent> finish = Flux.defer(() -> {
            String finalDelta = safetyWindow.finish();
            Flux<AgentStreamEvent> tail = finalDelta.isEmpty()
                    ? Flux.empty() : Flux.just(AgentStreamEvent.delta(finalDelta));
            return Flux.concat(tail,
                    Flux.just(AgentStreamEvent.stage(AgentStageUpdate.completed("SYNTHESIZING", "回答已整合完成"))),
                    recommendationEvents(prepared),
                    persistAndDone(userId, conversation, message, prepared,
                    safetyWindow.content(), start));
        });

        return Flux.concat(header, modelDeltas, finish)
                .doOnError(error -> {
                    auditRepository.complete(prepared.traceId(), System.currentTimeMillis() - start, "AGENT_STREAM_FAILED");
                    log.error("健康助手流式调用失败, sessionId={}", conversation.sessionId(), error);
                });
    }

    private Mono<AgentStreamEvent> persistAndDone(Long userId, AgentConversation conversation, String message,
                                                   PreparedAgentResponse prepared, String content, long start) {
        return Mono.fromRunnable(() -> {
                    healthAssistantChatMemory.add(conversation.conversationId(),
                            List.of(new UserMessage(message), new AssistantMessage(content)));
                    conversationRepository.touch(userId, conversation.sessionId());
                    auditRepository.saveTurn(userId, conversation.sessionId(), prepared.traceId(), message, content,
                            prepared.risk().level(), prepared.citations(), prepared.usedProfileCategories(),
                            prepared.recommendedDoctors());
                    completeIntakeIfFinished(userId, conversation.sessionId(), prepared);
                    auditRepository.complete(prepared.traceId(), System.currentTimeMillis() - start, null);
                    enqueueLongTermMemory(userId, conversation, message, prepared.traceId(), content);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(AgentStreamEvent.done());
    }

    private Execution execute(Long userId, AgentConversation conversation, String message, boolean useHealthProfile) {
        long start = System.currentTimeMillis();
        PreparedAgentResponse prepared = null;
        try {
            prepared = orchestrator.prepare(userId, conversation, message, useHealthProfile);
            String raw;
            if (prepared.direct()) {
                raw = prepared.directContent();
            } else {
                raw = healthAssistantChatClient.prompt()
                        .system(withLongTermMemory(userId, message, prepared.systemPrompt()))
                        .messages(healthAssistantChatMemory.get(conversation.conversationId()))
                        .user(message).call().content();
            }
            String content = addDoctorRecommendationOffer(enforceOutput(raw, prepared), prepared);
            healthAssistantChatMemory.add(conversation.conversationId(),
                    List.of(new UserMessage(message), new AssistantMessage(content)));
            conversationRepository.touch(userId, conversation.sessionId());
            auditRepository.saveTurn(userId, conversation.sessionId(), prepared.traceId(), message, content,
                    prepared.risk().level(), prepared.citations(), prepared.usedProfileCategories(),
                    prepared.recommendedDoctors());
            completeIntakeIfFinished(userId, conversation.sessionId(), prepared);
            auditRepository.complete(prepared.traceId(), System.currentTimeMillis() - start, null);
            enqueueLongTermMemory(userId, conversation, message, prepared.traceId(), content);
            return new Execution(prepared, content);
        } catch (Exception e) {
            if (prepared != null) auditRepository.complete(prepared.traceId(), System.currentTimeMillis() - start, "AGENT_EXECUTION_FAILED");
            log.error("健康助手调用失败, sessionId={}", conversation.sessionId(), e);
            throw e instanceof BusinessException be ? be : new BusinessException(friendlyMessage(e));
        }
    }

    private List<AgentStreamEvent> finalizationEvents(AgentConversation conversation, PreparedAgentResponse prepared) {
        List<AgentStreamEvent> events = new ArrayList<>();
        events.add(AgentStreamEvent.meta(conversation.sessionId(), prepared.traceId(), prepared.usedProfileCategories()));
        events.add(AgentStreamEvent.risk(prepared.risk().level(), prepared.risk().message()));
        if (prepared.intakeQuestion() != null) events.add(AgentStreamEvent.intake(prepared.intakeQuestion()));
        return events;
    }

    private List<AgentStreamEvent> textEvents(String content) {
        List<AgentStreamEvent> events = new ArrayList<>();
        int chunkSize = 24;
        for (int i = 0; i < content.length(); i += chunkSize) {
            events.add(AgentStreamEvent.delta(content.substring(i, Math.min(content.length(), i + chunkSize))));
        }
        return events;
    }

    private Flux<AgentStreamEvent> recommendationEvents(PreparedAgentResponse prepared) {
        if (prepared.recommendedDoctors() == null || prepared.recommendedDoctors().isEmpty()) return Flux.empty();
        return Flux.just(AgentStreamEvent.doctorRecommendations(prepared.recommendedDoctors()));
    }

    private String friendlyMessage(Throwable exception) {
        String detail = exception.getMessage();
        if (detail != null && (detail.contains("401") || detail.contains("api-key") || detail.contains("API key"))) {
            return "百炼密钥无效或未配置，请检查 APIKEY";
        }
        return "健康助手暂时不可用，请稍后再试";
    }

    private void completeIntakeIfFinished(Long userId, String sessionId, PreparedAgentResponse prepared) {
        if (!"CLARIFICATION".equals(prepared.route())) intakeStateRepository.complete(userId, sessionId);
    }

    private String addDoctorRecommendationOffer(String content, PreparedAgentResponse prepared) {
        if (!shouldOfferDoctorRecommendation(prepared) || content.contains("匹配平台内已审核的真实医生")) return content;
        return content + DOCTOR_RECOMMENDATION_OFFER;
    }

    private String enforceOutput(String content, PreparedAgentResponse prepared) {
        return "CLARIFICATION".equals(prepared.route())
                ? outputSafetyService.enforceClarification(content)
                : outputSafetyService.enforce(content, prepared.risk());
    }

    private boolean shouldOfferDoctorRecommendation(PreparedAgentResponse prepared) {
        String route = prepared.route() == null ? "" : prepared.route();
        return route.startsWith("CONSULTATION") && !route.contains("DOCTOR_TOOL")
                && !prepared.risk().emergency();
    }

    private String withLongTermMemory(Long userId, String message, String systemPrompt) {
        if (longTermMemoryService == null) return systemPrompt;
        return systemPrompt + longTermMemoryService.promptContext(userId, message, MemorySourceAgent.HEALTH);
    }

    private void enqueueLongTermMemory(Long userId, AgentConversation conversation, String message,
                                       String traceId, String content) {
        if (longTermMemoryService != null) {
            longTermMemoryService.enqueueTurn(userId, MemorySourceAgent.HEALTH, conversation.sessionId(), traceId,
                    message, content);
        }
    }

    private record Execution(PreparedAgentResponse prepared, String content) {}

    /**
     * 保留少量尾部字符后再下发，能够识别被模型拆到多个 token 中的敏感短语；
     * 其余内容仍随模型响应持续发送，不需要等待完整答案。
     */
    private static final class StreamingSafetyWindow {
        private static final int WINDOW_SIZE = 48;
        private final OutputSafetyService safetyService;
        private final RiskAssessment risk;
        private final boolean offerDoctorRecommendation;
        private final StringBuilder pending = new StringBuilder();
        private final StringBuilder emitted = new StringBuilder();

        private StreamingSafetyWindow(OutputSafetyService safetyService, RiskAssessment risk,
                                      boolean offerDoctorRecommendation) {
            this.safetyService = safetyService;
            this.risk = risk;
            this.offerDoctorRecommendation = offerDoctorRecommendation;
        }

        private String accept(String token) {
            pending.append(token);
            String sanitized = safetyService.sanitizeDiagnosisLanguage(pending.toString());
            pending.setLength(0);
            pending.append(sanitized);
            int emitLength = pending.length() - WINDOW_SIZE;
            if (emitLength <= 0) return "";
            String delta = pending.substring(0, emitLength);
            pending.delete(0, emitLength);
            emitted.append(delta);
            return delta;
        }

        private String finish() {
            String tail = safetyService.sanitizeDiagnosisLanguage(pending.toString());
            pending.setLength(0);
            String current = emitted + tail;
            String suffix = safetyService.completionSuffix(current, risk);
            String offer = offerDoctorRecommendation && !current.contains("匹配平台内已审核的真实医生")
                    ? DOCTOR_RECOMMENDATION_OFFER : "";
            String finalDelta = tail + suffix + offer;
            emitted.append(finalDelta);
            return finalDelta;
        }

        private String content() {
            return emitted.toString();
        }
    }
}
