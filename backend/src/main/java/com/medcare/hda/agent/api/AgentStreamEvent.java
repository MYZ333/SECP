package com.medcare.hda.agent.api;

import io.swagger.v3.oas.annotations.media.Schema;

/** SSE 流式聊天事件。 */
@Schema(description = "健康助手流式事件")
public record AgentStreamEvent(
        @Schema(description = "事件类型：meta/stage/risk/intake/citation/delta/done/error") String type,
        @Schema(description = "客户端会话 ID，仅 meta 事件提供") String sessionId,
        @Schema(description = "文本片段或提示信息") String content,
        @Schema(description = "执行阶段") String stage,
        @Schema(description = "阶段状态") String status,
        @Schema(description = "风险等级") String riskLevel,
        @Schema(description = "知识来源") AgentCitation citation,
        @Schema(description = "审计追踪 ID") String traceId,
        @Schema(description = "本次使用的健康档案类别") java.util.List<String> usedProfileCategories,
        @Schema(description = "需要用户回答的逐步问诊题目") AgentIntakeQuestion intakeQuestion
) {

    public static AgentStreamEvent meta(String sessionId) {
        return new AgentStreamEvent("meta", sessionId, null, null, null, null, null, null, java.util.List.of(), null);
    }

    public static AgentStreamEvent meta(String sessionId, String traceId, java.util.List<String> categories) {
        return new AgentStreamEvent("meta", sessionId, null, null, null, null, null, traceId, categories, null);
    }

    public static AgentStreamEvent stage(AgentStageUpdate update) {
        return new AgentStreamEvent("stage", null, update.message(), update.stage(), update.status(), null, null, null, java.util.List.of(), null);
    }

    public static AgentStreamEvent risk(String level, String message) {
        return new AgentStreamEvent("risk", null, message, "SAFETY_CHECK", "COMPLETED", level, null, null, java.util.List.of(), null);
    }

    public static AgentStreamEvent citation(AgentCitation citation) {
        return new AgentStreamEvent("citation", null, null, null, null, null, citation, null, java.util.List.of(), null);
    }

    public static AgentStreamEvent intake(AgentIntakeQuestion question) {
        return new AgentStreamEvent("intake", null, null, "CLARIFYING", "WAITING", null, null, null,
                java.util.List.of(), question);
    }

    public static AgentStreamEvent delta(String content) {
        return new AgentStreamEvent("delta", null, content, null, null, null, null, null, java.util.List.of(), null);
    }

    public static AgentStreamEvent done() {
        return new AgentStreamEvent("done", null, null, null, null, null, null, null, java.util.List.of(), null);
    }

    public static AgentStreamEvent error(String message) {
        return new AgentStreamEvent("error", null, message, null, null, null, null, null, java.util.List.of(), null);
    }
}
