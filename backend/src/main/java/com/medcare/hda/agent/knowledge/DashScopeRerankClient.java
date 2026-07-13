package com.medcare.hda.agent.knowledge;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class DashScopeRerankClient {
    private final boolean enabled;
    private final String workspaceId;
    private final String apiKey;
    private final String model;
    private final RestClient restClient = RestClient.create();

    public DashScopeRerankClient(@Value("${hda.agent.rerank.enabled:true}") boolean enabled,
                                 @Value("${hda.agent.rerank.workspace-id:}") String workspaceId,
                                 @Value("${hda.agent.rerank.api-key:}") String apiKey,
                                 @Value("${hda.agent.rerank.model:qwen3-rerank}") String model) {
        this.enabled = enabled;
        this.workspaceId = workspaceId;
        this.apiKey = apiKey;
        this.model = model;
    }

    public List<ScoredIndex> rerank(String query, List<String> documents, int topN) {
        if (!enabled || !StringUtils.hasText(workspaceId) || !StringUtils.hasText(apiKey) || documents.isEmpty()) {
            return fallback(documents.size(), topN);
        }
        try {
            String url = "https://" + workspaceId + ".cn-beijing.maas.aliyuncs.com/compatible-api/v1/reranks";
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("query", query);
            body.put("documents", documents);
            body.put("top_n", Math.min(topN, documents.size()));
            body.put("instruct", "Given a health question, retrieve authoritative passages that directly answer it.");
            JsonNode response = restClient.post().uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(body).retrieve().body(JsonNode.class);
            List<ScoredIndex> scored = new ArrayList<>();
            if (response != null) {
                for (JsonNode item : response.path("results")) {
                    scored.add(new ScoredIndex(item.path("index").asInt(), item.path("relevance_score").asDouble()));
                }
            }
            return scored.isEmpty() ? fallback(documents.size(), topN) : scored;
        } catch (Exception e) {
            log.warn("百炼重排序不可用，降级为向量排序: {}", e.getMessage());
            return fallback(documents.size(), topN);
        }
    }

    private List<ScoredIndex> fallback(int size, int topN) {
        List<ScoredIndex> result = new ArrayList<>();
        for (int i = 0; i < Math.min(size, topN); i++) result.add(new ScoredIndex(i, 1D - i * 0.01));
        result.sort(Comparator.comparingDouble(ScoredIndex::score).reversed());
        return result;
    }

    public record ScoredIndex(int index, double score) {}
}
