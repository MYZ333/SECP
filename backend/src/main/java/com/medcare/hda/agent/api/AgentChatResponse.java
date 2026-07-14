package com.medcare.hda.agent.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** 健康助手同步回复，字段与旧聊天接口保持兼容。 */
@Schema(description = "健康助手聊天回复")
public record AgentChatResponse(
        @Schema(description = "客户端会话 ID") String sessionId,
        @Schema(description = "助手回复内容") String content,
        @Schema(description = "消息角色") String role,
        @Schema(description = "风险等级") String riskLevel,
        @Schema(description = "知识来源") List<AgentCitation> citations,
        @Schema(description = "本次使用的健康档案类别") List<String> usedProfileCategories,
        @Schema(description = "审计追踪 ID") String traceId,
        @Schema(description = "需要用户回答的逐步问诊题目") AgentIntakeQuestion intakeQuestion
) {
}
