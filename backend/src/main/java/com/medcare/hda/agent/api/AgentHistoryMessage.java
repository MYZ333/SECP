package com.medcare.hda.agent.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/** 健康助手持久化历史消息。 */
@Schema(description = "健康助手历史消息")
public record AgentHistoryMessage(
        @Schema(description = "消息角色：user/assistant/system") String role,
        @Schema(description = "消息内容") String content,
        @Schema(description = "创建时间") LocalDateTime createTime,
        @Schema(description = "客户端会话 ID") String sessionId,
        @Schema(description = "风险等级，仅助手消息提供") String riskLevel,
        @Schema(description = "知识来源，仅助手消息提供") List<AgentCitation> citations,
        @Schema(description = "本次使用的健康档案类别") List<String> usedProfileCategories,
        @Schema(description = "审计追踪 ID") String traceId
) {
}
