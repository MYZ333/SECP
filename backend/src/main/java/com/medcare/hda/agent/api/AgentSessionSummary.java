package com.medcare.hda.agent.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "健康助手历史会话摘要")
public record AgentSessionSummary(
        @Schema(description = "客户端会话 ID") String sessionId,
        @Schema(description = "会话标题，取首次用户提问") String title,
        @Schema(description = "最近更新时间") LocalDateTime updateTime
) {
}
