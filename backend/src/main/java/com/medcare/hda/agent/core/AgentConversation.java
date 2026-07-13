package com.medcare.hda.agent.core;

/** 对外会话 ID 与 Spring AI 内部会话 ID 的安全映射。 */
public record AgentConversation(String sessionId, String conversationId) {
}
