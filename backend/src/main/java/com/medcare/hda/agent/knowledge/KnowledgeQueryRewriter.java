package com.medcare.hda.agent.knowledge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Rewrites conversational questions into standalone retrieval queries and sparse-search keywords. */
@Slf4j
@Component
public class KnowledgeQueryRewriter {
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final String apiKey;
    private final int maxKeywords;

    public KnowledgeQueryRewriter(@Qualifier("healthAssistantChatClient") ChatClient chatClient,
                                  ObjectMapper objectMapper,
                                  @Value("${hda.agent.rag.query-rewrite.enabled:true}") boolean enabled,
                                  @Value("${spring.ai.dashscope.api-key:}") String apiKey,
                                  @Value("${hda.agent.rag.query-rewrite.max-keywords:6}") int maxKeywords) {
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.apiKey = apiKey;
        this.maxKeywords = Math.max(1, Math.min(12, maxKeywords));
    }

    public RewrittenQuery rewrite(String query, String agentType) {
        String original = normalize(query, 500);
        RewrittenQuery fallback = fallback(original, agentType);
        if (!enabled || !hasUsableApiKey() || !StringUtils.hasText(original)) return fallback;
        try {
            String content = chatClient.prompt()
                    .system("""
                            你是 RAG 检索查询改写器。将用户问题改写成脱离对话也能理解的简洁检索句，
                            保留疾病、症状、药物、时间、适用人群、功能名称等关键限定，不回答问题，不添加未经提及的事实。
                            只输出 JSON：{"query":"...","keywords":["...","..."]}。
                            keywords 为 2 到 6 个适合关键词检索的中文短语。
                            """)
                    .user("知识库类型：" + normalizedAgentType(agentType) + "\n用户问题：" + original)
                    .call().content();
            JsonNode json = objectMapper.readTree(stripCodeFence(content));
            String rewritten = normalize(json.path("query").asText(), 500);
            if (!StringUtils.hasText(rewritten)) return fallback;
            List<String> keywords = new ArrayList<>();
            json.path("keywords").forEach(node -> addKeyword(keywords, node.asText()));
            fallback.keywords().forEach(keyword -> addKeyword(keywords, keyword));
            return new RewrittenQuery(original, rewritten, keywords.stream().limit(maxKeywords).toList(), true);
        } catch (Exception error) {
            log.warn("Query 改写不可用，使用规则改写: {}", error.getMessage());
            return fallback;
        }
    }

    private RewrittenQuery fallback(String original, String agentType) {
        String suffix = "APPLICATION".equalsIgnoreCase(agentType)
                ? " 功能说明 使用方法 操作步骤" : " 健康科普 科学就医";
        Set<String> keywords = new LinkedHashSet<>();
        if (StringUtils.hasText(original)) keywords.add(original);
        for (String token : original.split("[\\s，。！？、；：,.!?;:]+")) {
            String value = normalize(token, 40);
            if (value.length() >= 2) keywords.add(value);
            if (keywords.size() >= maxKeywords) break;
        }
        String semanticQuery = StringUtils.hasText(original) ? normalize(original + suffix, 500) : "";
        return new RewrittenQuery(original, semanticQuery, List.copyOf(keywords), false);
    }

    private void addKeyword(List<String> keywords, String value) {
        String normalized = normalize(value, 40);
        if (normalized.length() >= 2 && !keywords.contains(normalized) && keywords.size() < maxKeywords) {
            keywords.add(normalized);
        }
    }

    private boolean hasUsableApiKey() {
        return StringUtils.hasText(apiKey) && !apiKey.toLowerCase(Locale.ROOT).contains("placeholder");
    }

    private String stripCodeFence(String value) {
        if (value == null) return "{}";
        String text = value.trim();
        if (!text.startsWith("```")) return text;
        int firstLine = text.indexOf('\n');
        int lastFence = text.lastIndexOf("```");
        return firstLine >= 0 && lastFence > firstLine ? text.substring(firstLine + 1, lastFence).trim() : text;
    }

    private String normalize(String value, int maxLength) {
        if (value == null) return "";
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.substring(0, Math.min(normalized.length(), maxLength));
    }

    private String normalizedAgentType(String agentType) {
        return "APPLICATION".equalsIgnoreCase(agentType) ? "应用使用知识库" : "健康知识库";
    }

    public record RewrittenQuery(String originalQuery, String semanticQuery, List<String> keywords,
                                 boolean modelRewritten) { }
}
