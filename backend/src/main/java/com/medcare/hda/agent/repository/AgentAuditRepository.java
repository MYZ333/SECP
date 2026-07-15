package com.medcare.hda.agent.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medcare.hda.agent.api.AgentCitation;
import com.medcare.hda.agent.api.DoctorRecommendation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class AgentAuditRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public void start(String traceId, Long userId, String sessionId, boolean useProfile) {
        safe(() -> jdbcTemplate.update("""
                INSERT INTO agent_run(trace_id,user_id,session_id,use_health_profile,status)
                VALUES(?,?,?,?,'RUNNING')
                """, traceId, userId, sessionId, useProfile ? 1 : 0));
    }

    public void route(String traceId, String route, String risk) {
        safe(() -> jdbcTemplate.update("UPDATE agent_run SET route=?,risk_level=? WHERE trace_id=?", route, risk, traceId));
    }

    public void step(String traceId, String agent, String status, String summary, long latencyMs) {
        safe(() -> jdbcTemplate.update("""
                INSERT INTO agent_run_step(trace_id,agent_type,status,summary,latency_ms) VALUES(?,?,?,?,?)
                """, traceId, agent, status, abbreviate(summary, 900), latencyMs));
    }

    public void complete(String traceId, long latencyMs, String errorCode) {
        safe(() -> jdbcTemplate.update("UPDATE agent_run SET status=?,latency_ms=?,error_code=? WHERE trace_id=?",
                errorCode == null ? "COMPLETED" : "FAILED", latencyMs, errorCode, traceId));
    }

    public void saveTurn(Long userId, String sessionId, String traceId, String question, String answer,
                         String risk, List<AgentCitation> citations, List<String> categories,
                         List<DoctorRecommendation> recommendedDoctors) {
        safe(() -> jdbcTemplate.update("""
                INSERT INTO agent_chat_turn(user_id,session_id,trace_id,question,answer,risk_level,citations_json,
                                            profile_categories_json,doctor_recommendations_json)
                VALUES(?,?,?,?,?,?,?,?,?)
                """, userId, sessionId, traceId, question, answer, risk, json(citations), json(categories),
                json(recommendedDoctors)));
    }

    /**
     * Only the latest assistant turn is considered. This prevents an old doctor
     * offer in the same session from turning an unrelated short reply such as
     * "好的" into a recommendation request.
     */
    public boolean wasDoctorRecommendationOffered(Long userId, String sessionId) {
        try {
            List<Integer> values = jdbcTemplate.queryForList("""
                    SELECT CASE WHEN answer LIKE ? THEN 1 ELSE 0 END
                    FROM agent_chat_turn
                    WHERE user_id=? AND session_id=?
                    ORDER BY create_time DESC,id DESC
                    LIMIT 1
                    """, Integer.class, "%匹配平台内已审核的真实医生%", userId, sessionId);
            return !values.isEmpty() && values.getFirst() == 1;
        } catch (Exception error) {
            log.warn("读取医生推荐邀请状态失败，按未邀请处理: {}", error.getMessage());
            return false;
        }
    }

    private String json(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException e) { return "[]"; }
    }
    private String abbreviate(String value, int max) {
        if (value == null) return null; return value.substring(0, Math.min(value.length(), max));
    }
    private void safe(Runnable action) {
        try { action.run(); } catch (Exception e) { log.warn("Agent 审计写入失败，不影响主流程: {}", e.getMessage()); }
    }
}
