package com.medcare.hda.agent.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medcare.hda.agent.api.AgentCitation;
import com.medcare.hda.agent.api.AgentHistoryMessage;
import com.medcare.hda.agent.api.AgentSessionSummary;
import com.medcare.hda.agent.core.AgentConversation;
import com.medcare.hda.common.PageResult;
import com.medcare.hda.common.ResultCode;
import com.medcare.hda.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 保存用户与 Spring AI 内部 conversationId 的授权映射；实际聊天消息始终存放于
 * SPRING_AI_CHAT_MEMORY，当前表不保存任何消息内容。
 */
@Repository
@RequiredArgsConstructor
public class AgentConversationRepository {

    private static final int MAX_SESSIONS_PER_USER = 40;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public AgentConversation resolve(Long userId, String requestedSessionId) {
        String sessionId = StringUtils.hasText(requestedSessionId) ? requestedSessionId : UUID.randomUUID().toString();
        validateSessionId(sessionId);

        List<String> existing = jdbcTemplate.query(
                "SELECT conversation_id FROM agent_chat_session WHERE user_id = ? AND session_id = ?",
                (rs, rowNum) -> rs.getString(1), userId, sessionId);
        if (!existing.isEmpty()) {
            touch(userId, sessionId);
            return new AgentConversation(sessionId, existing.getFirst());
        }

        String conversationId = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                INSERT INTO agent_chat_session (user_id, session_id, conversation_id, create_time, update_time)
                VALUES (?, ?, ?, NOW(), NOW())
                """, userId, sessionId, conversationId);
        return new AgentConversation(sessionId, conversationId);
    }

    public List<AgentSessionSummary> listSessions(Long userId) {
        return jdbcTemplate.query("""
                SELECT s.session_id,
                       COALESCE(
                           (SELECT t.question FROM agent_chat_turn t
                            WHERE t.user_id = s.user_id AND t.session_id = s.session_id
                            ORDER BY t.create_time ASC LIMIT 1),
                           (SELECT m.content FROM SPRING_AI_CHAT_MEMORY m
                            WHERE m.conversation_id = s.conversation_id AND m.type = 'USER'
                            ORDER BY m.timestamp ASC LIMIT 1)
                       ) AS title,
                       s.update_time
                FROM agent_chat_session s
                WHERE s.user_id = ?
                ORDER BY s.update_time DESC, s.id DESC
                LIMIT ?
                """,
                (rs, rowNum) -> new AgentSessionSummary(
                        rs.getString("session_id"),
                        rs.getString("title"),
                        toLocalDateTime(rs.getTimestamp("update_time"))),
                userId, MAX_SESSIONS_PER_USER);
    }

    @Transactional
    public void touch(Long userId, String sessionId) {
        jdbcTemplate.update("UPDATE agent_chat_session SET update_time = NOW() WHERE user_id = ? AND session_id = ?",
                userId, sessionId);
        pruneOldestSessions(userId);
    }

    private void pruneOldestSessions(Long userId) {
        List<SessionStorage> expired = jdbcTemplate.query("""
                SELECT session_id, conversation_id
                FROM agent_chat_session
                WHERE user_id = ?
                ORDER BY update_time DESC, id DESC
                LIMIT 1000000 OFFSET ?
                """,
                (rs, rowNum) -> new SessionStorage(rs.getString("session_id"), rs.getString("conversation_id")),
                userId, MAX_SESSIONS_PER_USER);

        for (SessionStorage session : expired) {
            jdbcTemplate.update("DELETE FROM agent_consult_state WHERE user_id = ? AND session_id = ?",
                    userId, session.sessionId());
            jdbcTemplate.update("DELETE FROM agent_chat_turn WHERE user_id = ? AND session_id = ?",
                    userId, session.sessionId());
            jdbcTemplate.update("DELETE FROM SPRING_AI_CHAT_MEMORY WHERE conversation_id = ?",
                    session.conversationId());
            jdbcTemplate.update("DELETE FROM agent_chat_session WHERE user_id = ? AND session_id = ?",
                    userId, session.sessionId());
        }
    }

    public PageResult<AgentHistoryMessage> pageHistory(Long userId, String sessionId, long pageNum, long pageSize) {
        if (StringUtils.hasText(sessionId)) {
            validateSessionId(sessionId);
        }
        long current = Math.max(pageNum, 1);
        long size = Math.min(Math.max(pageSize, 1), 100);
        String filter = StringUtils.hasText(sessionId) ? " AND s.session_id = ?" : "";
        Object[] parameters = StringUtils.hasText(sessionId) ? new Object[]{userId, sessionId} : new Object[]{userId};

        Long total = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM agent_chat_session s
                JOIN SPRING_AI_CHAT_MEMORY m ON m.conversation_id = s.conversation_id
                WHERE s.user_id = ?
                """ + filter, Long.class, parameters);

        List<AgentHistoryMessage> records = jdbcTemplate.query("""
                SELECT s.session_id, m.type, m.content, m.timestamp,
                       (SELECT t.risk_level FROM agent_chat_turn t
                        WHERE t.user_id=s.user_id AND t.session_id=s.session_id AND t.answer=m.content
                        ORDER BY t.create_time DESC LIMIT 1) AS risk_level,
                       (SELECT CAST(t.citations_json AS CHAR) FROM agent_chat_turn t
                        WHERE t.user_id=s.user_id AND t.session_id=s.session_id AND t.answer=m.content
                        ORDER BY t.create_time DESC LIMIT 1) AS citations_json,
                       (SELECT CAST(t.profile_categories_json AS CHAR) FROM agent_chat_turn t
                        WHERE t.user_id=s.user_id AND t.session_id=s.session_id AND t.answer=m.content
                        ORDER BY t.create_time DESC LIMIT 1) AS profile_categories_json,
                       (SELECT t.trace_id FROM agent_chat_turn t
                        WHERE t.user_id=s.user_id AND t.session_id=s.session_id AND t.answer=m.content
                        ORDER BY t.create_time DESC LIMIT 1) AS trace_id
                FROM agent_chat_session s
                JOIN SPRING_AI_CHAT_MEMORY m ON m.conversation_id = s.conversation_id
                WHERE s.user_id = ?
                """ + filter + " ORDER BY m.timestamp ASC LIMIT ? OFFSET ?",
                (rs, rowNum) -> new AgentHistoryMessage(
                        role(rs.getString("type")),
                        rs.getString("content"),
                        toLocalDateTime(rs.getTimestamp("timestamp")),
                        rs.getString("session_id"),
                        rs.getString("risk_level"),
                        parseCitations(rs.getString("citations_json")),
                        parseStrings(rs.getString("profile_categories_json")),
                        rs.getString("trace_id")),
                concat(parameters, size, (current - 1) * size));

        PageResult<AgentHistoryMessage> result = new PageResult<>();
        result.setTotal(total == null ? 0 : total);
        result.setRecords(records);
        result.setCurrent(current);
        result.setSize(size);
        return result;
    }

    private Object[] concat(Object[] parameters, Object... tail) {
        Object[] result = new Object[parameters.length + tail.length];
        System.arraycopy(parameters, 0, result, 0, parameters.length);
        System.arraycopy(tail, 0, result, parameters.length, tail.length);
        return result;
    }

    private String role(String type) {
        return switch (type) {
            case "USER" -> "user";
            case "ASSISTANT" -> "assistant";
            case "SYSTEM" -> "system";
            default -> "tool";
        };
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private List<AgentCitation> parseCitations(String json) {
        if (!StringUtils.hasText(json)) return List.of();
        try { return objectMapper.readValue(json, new TypeReference<>() {}); }
        catch (Exception ignored) { return List.of(); }
    }

    private List<String> parseStrings(String json) {
        if (!StringUtils.hasText(json)) return List.of();
        try { return objectMapper.readValue(json, new TypeReference<>() {}); }
        catch (Exception ignored) { return List.of(); }
    }

    private void validateSessionId(String sessionId) {
        try {
            UUID.fromString(sessionId);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "sessionId 必须是 UUID 格式");
        }
    }

    private record SessionStorage(String sessionId, String conversationId) {}
}
