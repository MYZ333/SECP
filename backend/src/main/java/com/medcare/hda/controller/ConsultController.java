package com.medcare.hda.controller;

import com.medcare.hda.agent.api.AgentChatResponse;
import com.medcare.hda.agent.api.AgentHistoryMessage;
import com.medcare.hda.agent.api.AgentSessionSummary;
import com.medcare.hda.agent.api.AgentStreamEvent;
import com.medcare.hda.agent.core.AgentConversation;
import com.medcare.hda.agent.core.HealthAssistantAgentService;
import com.medcare.hda.agent.repository.AgentConversationRepository;
import com.medcare.hda.annotation.RateLimit;
import com.medcare.hda.common.PageResult;
import com.medcare.hda.common.Result;
import com.medcare.hda.common.ratelimit.LimitDimension;
import com.medcare.hda.dto.ChatDTO;
import com.medcare.hda.security.SecurityUtil;
import com.medcare.hda.service.AsyncTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

@Tag(name = "健康咨询", description = "健康助手 Agent 对话")
@RestController
@RequestMapping("/api/consult")
@RequiredArgsConstructor
public class ConsultController {

    private final HealthAssistantAgentService healthAssistantAgentService;
    private final AgentConversationRepository conversationRepository;
    private final AsyncTaskService asyncTaskService;

    @Operation(summary = "发起健康咨询（同步兼容接口）")
    @RateLimit(key = "ai-consult", window = 60, limit = 10, dimension = LimitDimension.USER,
            message = "AI 咨询太频繁，请稍后再问")
    @PostMapping("/chat")
    public Result<AgentChatResponse> chat(@Valid @RequestBody ChatDTO dto) {
        Long userId = SecurityUtil.getUserId();
        AgentConversation conversation = healthAssistantAgentService.prepareConversation(userId, dto.getSessionId());
        AgentChatResponse response = healthAssistantAgentService.chat(userId, conversation, dto.getMessage(), dto.isUseHealthProfile());
        asyncTaskService.markTaskReadyAsync(userId, "CONSULT");
        return Result.success(response);
    }

    @Operation(summary = "发起健康咨询（SSE 流式接口）")
    @RateLimit(key = "ai-consult", window = 60, limit = 10, dimension = LimitDimension.USER,
            message = "AI 咨询太频繁，请稍后再问")
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AgentStreamEvent>> stream(@Valid @RequestBody ChatDTO dto,
                                                          HttpServletResponse response) {
        Long userId = SecurityUtil.getUserId();
        response.setHeader("Cache-Control", "no-cache, no-transform");
        response.setHeader("X-Accel-Buffering", "no");
        AgentConversation conversation = healthAssistantAgentService.prepareConversation(userId, dto.getSessionId());
        return healthAssistantAgentService
                .stream(userId, conversation, dto.getMessage(), dto.isUseHealthProfile())
                .map(data -> event(data.type(), data))
                .doOnComplete(() -> asyncTaskService.markTaskReadyAsync(userId, "CONSULT"))
                .onErrorResume(error -> Flux.just(event("error",
                        AgentStreamEvent.error("健康助手暂时不可用，请稍后再试"))));
    }

    @Operation(summary = "健康助手历史对话（分页）")
    @GetMapping("/history")
    public Result<PageResult<AgentHistoryMessage>> history(@RequestParam(required = false) String sessionId,
                                                            @RequestParam(defaultValue = "1") long pageNum,
                                                            @RequestParam(defaultValue = "20") long pageSize) {
        Long userId = SecurityUtil.getUserId();
        return Result.success(conversationRepository.pageHistory(userId, sessionId, pageNum, pageSize));
    }

    @Operation(summary = "健康助手最近会话列表")
    @GetMapping("/sessions")
    public Result<List<AgentSessionSummary>> sessions() {
        return Result.success(conversationRepository.listSessions(SecurityUtil.getUserId()));
    }

    @Operation(summary = "删除健康助手对话")
    @DeleteMapping("/sessions/{sessionId}")
    public Result<Void> deleteSession(@PathVariable String sessionId) {
        healthAssistantAgentService.deleteConversation(SecurityUtil.getUserId(), sessionId);
        return Result.success("对话已删除", null);
    }

    private ServerSentEvent<AgentStreamEvent> event(String name, AgentStreamEvent data) {
        return ServerSentEvent.<AgentStreamEvent>builder()
                .event(name)
                .data(data)
                .build();
    }
}
