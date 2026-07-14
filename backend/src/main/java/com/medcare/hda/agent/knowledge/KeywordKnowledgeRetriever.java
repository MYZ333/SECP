package com.medcare.hda.agent.knowledge;

import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Sparse side of hybrid retrieval. It searches published chunks and returns a ranked candidate list. */
@Component
public class KeywordKnowledgeRetriever {
    private final JdbcTemplate jdbcTemplate;
    private final boolean enabled;
    private final int topK;

    public KeywordKnowledgeRetriever(JdbcTemplate jdbcTemplate,
                                     @Value("${hda.agent.rag.hybrid.enabled:true}") boolean enabled,
                                     @Value("${hda.agent.rag.hybrid.keyword-top-k:20}") int topK) {
        this.jdbcTemplate = jdbcTemplate;
        this.enabled = enabled;
        this.topK = Math.max(1, Math.min(100, topK));
    }

    public List<Document> search(List<String> rawKeywords, String agentType) {
        List<String> keywords = normalizeKeywords(rawKeywords);
        if (!enabled || keywords.isEmpty()) return List.of();
        String scoreExpression = keywords.stream()
                .map(ignored -> "(CASE WHEN kd.title LIKE ? THEN 4 ELSE 0 END + "
                        + "CASE WHEN kc.section_title LIKE ? THEN 3 ELSE 0 END + "
                        + "CASE WHEN kc.content LIKE ? THEN 1 ELSE 0 END)")
                .reduce((left, right) -> left + " + " + right).orElse("0");
        String matchExpression = keywords.stream()
                .map(ignored -> "(kd.title LIKE ? OR kc.section_title LIKE ? OR kc.content LIKE ?)")
                .reduce((left, right) -> left + " OR " + right).orElse("1=0");
        String sql = """
                SELECT kc.id, kc.content, kc.section_title, kd.id AS document_id, kd.title, kd.source_org,
                       kd.source_url, kd.published_date, kd.category, kd.agent_type,
                """ + scoreExpression + """
                       AS lexical_score
                FROM knowledge_chunk kc
                JOIN knowledge_document kd ON kd.id=kc.document_id
                WHERE kc.deleted=0 AND kd.deleted=0 AND kc.status='PUBLISHED' AND kd.status='PUBLISHED'
                  AND kd.agent_type=? AND (
                """ + matchExpression + ") ORDER BY lexical_score DESC, kd.update_time DESC LIMIT ?";

        List<Object> params = new ArrayList<>();
        addLikeParams(params, keywords);
        params.add(normalizedAgentType(agentType));
        addLikeParams(params, keywords);
        params.add(topK);
        return jdbcTemplate.query(sql, (rs, rowNum) -> Document.builder()
                .id("knowledge-" + rs.getLong("id"))
                .text(rs.getString("content"))
                .metadata(Map.of(
                        "documentId", rs.getLong("document_id"),
                        "title", nonNull(rs.getString("title")),
                        "sourceOrg", nonNull(rs.getString("source_org")),
                        "sourceUrl", nonNull(rs.getString("source_url")),
                        "publishedDate", rs.getDate("published_date") == null ? "" : rs.getDate("published_date").toString(),
                        "category", nonNull(rs.getString("category")),
                        "agentType", nonNull(rs.getString("agent_type")),
                        "section", rs.getString("section_title") == null ? "正文" : rs.getString("section_title"),
                        "lexicalScore", rs.getDouble("lexical_score")))
                .build(), params.toArray());
    }

    private void addLikeParams(List<Object> params, List<String> keywords) {
        for (String keyword : keywords) {
            String like = "%" + keyword + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
    }

    private List<String> normalizeKeywords(List<String> rawKeywords) {
        if (rawKeywords == null) return List.of();
        Set<String> result = new LinkedHashSet<>();
        for (String keyword : rawKeywords) {
            if (!StringUtils.hasText(keyword)) continue;
            String normalized = keyword.replaceAll("[\\r\\n%_]", " ").replaceAll("\\s+", " ").trim();
            if (normalized.length() >= 2) result.add(normalized.substring(0, Math.min(40, normalized.length())));
            if (result.size() >= 8) break;
        }
        return List.copyOf(result);
    }

    private String normalizedAgentType(String agentType) {
        return "APPLICATION".equalsIgnoreCase(agentType) ? "APPLICATION" : "HEALTH";
    }

    private String nonNull(String value) {
        return value == null ? "" : value;
    }
}
