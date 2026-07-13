package com.medcare.hda.controller;

import com.medcare.hda.agent.api.AgentStreamEvent;
import com.medcare.hda.agent.core.ApplicationAssistantService;
import com.medcare.hda.annotation.RateLimit;
import com.medcare.hda.common.ratelimit.LimitDimension;
import com.medcare.hda.dto.ApplicationAssistantChatDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@Tag(name = "应用使用助手", description = "应用使用方式 AI 对话")
@RestController
@RequestMapping("/api/app-assistant")
@RequiredArgsConstructor
public class ApplicationAssistantController {

    private final ApplicationAssistantService applicationAssistantService;

    @Operation(summary = "向应用使用助手提问（SSE 流式接口）")
    @RateLimit(key = "app-assistant", window = 60, limit = 10, dimension = LimitDimension.USER,
            message = "应用助手请求太频繁，请稍后再问")
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AgentStreamEvent>> stream(
            @Valid @RequestBody ApplicationAssistantChatDTO dto,
            HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-transform");
        response.setHeader("X-Accel-Buffering", "no");
        return applicationAssistantService.stream(dto.getMessage())
                .map(data -> event(data.type(), data))
                .onErrorResume(error -> Flux.just(event("error",
                        AgentStreamEvent.error("应用助手暂时不可用，请稍后再试"))));
    }

    private ServerSentEvent<AgentStreamEvent> event(String name, AgentStreamEvent data) {
        return ServerSentEvent.<AgentStreamEvent>builder()
                .event(name)
                .data(data)
                .build();
    }
}
