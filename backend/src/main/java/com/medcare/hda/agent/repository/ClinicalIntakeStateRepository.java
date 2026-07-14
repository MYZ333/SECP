package com.medcare.hda.agent.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medcare.hda.agent.core.ClinicalIntakeAssessment;
import com.medcare.hda.agent.core.ClinicalIntakeState;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ClinicalIntakeStateRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public Optional<ClinicalIntakeState> findActive(Long userId, String sessionId) {
        List<ClinicalIntakeState> states = jdbcTemplate.query("""
                SELECT user_id,session_id,episode_id,phase,round_count,initial_question,clinical_summary,
                       CAST(known_facts_json AS CHAR) known_facts_json,
                       CAST(missing_fields_json AS CHAR) missing_fields_json
                FROM agent_consult_state
                WHERE user_id=? AND session_id=? AND phase='COLLECTING'
                """, (rs, rowNum) -> new ClinicalIntakeState(
                rs.getLong("user_id"), rs.getString("session_id"), rs.getString("episode_id"),
                rs.getString("phase"), rs.getInt("round_count"), rs.getString("initial_question"),
                rs.getString("clinical_summary"), parseList(rs.getString("known_facts_json")),
                parseList(rs.getString("missing_fields_json"))), userId, sessionId);
        return states.stream().findFirst();
    }

    public void saveClarification(Long userId, String sessionId, String currentMessage,
                                  ClinicalIntakeState existing, ClinicalIntakeAssessment assessment) {
        boolean startNew = existing == null || assessment.newEpisode();
        String episodeId = startNew ? UUID.randomUUID().toString() : existing.episodeId();
        int roundCount = startNew ? 1 : existing.roundCount() + 1;
        String initialQuestion = startNew ? currentMessage : existing.initialQuestion();
        String summary = assessment.clinicalSummary();
        if (assessment.question() != null) {
            summary = (summary == null ? "" : summary) + "\n待回答问题：" + assessment.question().prompt();
        }
        jdbcTemplate.update("""
                INSERT INTO agent_consult_state
                    (user_id,session_id,episode_id,phase,round_count,initial_question,clinical_summary,
                     known_facts_json,missing_fields_json,create_time,update_time)
                VALUES(?,?,?,'COLLECTING',?,?,?,?,?,NOW(),NOW())
                ON DUPLICATE KEY UPDATE
                    episode_id=VALUES(episode_id),phase='COLLECTING',round_count=VALUES(round_count),
                    initial_question=VALUES(initial_question),clinical_summary=VALUES(clinical_summary),
                    known_facts_json=VALUES(known_facts_json),missing_fields_json=VALUES(missing_fields_json),
                    update_time=NOW()
                """, userId, sessionId, episodeId, roundCount, initialQuestion,
                summary, json(assessment.knownFacts()), json(assessment.missingFields()));
    }

    public void complete(Long userId, String sessionId) {
        jdbcTemplate.update("""
                UPDATE agent_consult_state SET phase='COMPLETED',update_time=NOW()
                WHERE user_id=? AND session_id=? AND phase='COLLECTING'
                """, userId, sessionId);
    }

    private String json(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (Exception ignored) {
            return "[]";
        }
    }

    private List<String> parseList(String value) {
        if (value == null || value.isBlank()) return List.of();
        try {
            return objectMapper.readValue(value, new TypeReference<>() { });
        } catch (Exception ignored) {
            return List.of();
        }
    }
}
