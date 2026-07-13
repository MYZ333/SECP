package com.medcare.hda.agent.knowledge;

import com.medcare.hda.agent.api.AgentCitation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class KnowledgeRetrievalService {
    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final DashScopeRerankClient reranker;
    private final boolean enabled;
    private final int vectorTopK;
    private final int finalTopK;
    private final double minScore;

    public KnowledgeRetrievalService(ObjectProvider<VectorStore> vectorStoreProvider,
                                     DashScopeRerankClient reranker,
                                     @Value("${hda.agent.rag.enabled:true}") boolean enabled,
                                     @Value("${hda.agent.rag.vector-top-k:30}") int vectorTopK,
                                     @Value("${hda.agent.rag.final-top-k:5}") int finalTopK,
                                     @Value("${hda.agent.rag.min-score:0.35}") double minScore) {
        this.vectorStoreProvider = vectorStoreProvider;
        this.reranker = reranker;
        this.enabled = enabled;
        this.vectorTopK = vectorTopK;
        this.finalTopK = finalTopK;
        this.minScore = minScore;
    }

    public List<KnowledgeHit> search(String query) {
        return search(query, "HEALTH");
    }

    public List<KnowledgeHit> search(String query, String agentType) {
        if (!enabled) return List.of();
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
            log.warn("RAG 已启用但没有可用的 VectorStore");
            return List.of();
        }
        try {
            List<Document> candidates = vectorStore.similaritySearch(SearchRequest.builder()
                    .query(query).topK(vectorTopK).build());
            if (candidates == null || candidates.isEmpty()) return List.of();
            List<Document> scoped = candidates.stream()
                    .filter(document -> belongsToAgent(document, agentType))
                    .toList();
            if (scoped.isEmpty()) return List.of();
            List<String> texts = scoped.stream().map(Document::getText).toList();
            List<DashScopeRerankClient.ScoredIndex> scored = reranker.rerank(query, texts, Math.min(8, scoped.size()));
            List<KnowledgeHit> hits = new ArrayList<>();
            for (DashScopeRerankClient.ScoredIndex score : scored) {
                if (hits.size() >= finalTopK || score.index() < 0 || score.index() >= scoped.size()) break;
                if (score.score() < minScore) continue;
                Document doc = scoped.get(score.index());
                Map<String, Object> meta = doc.getMetadata();
                AgentCitation citation = new AgentCitation(
                        asLong(meta.get("documentId")), asString(meta.get("title")), asString(meta.get("sourceOrg")),
                        asString(meta.get("sourceUrl")), asString(meta.get("publishedDate")), asString(meta.get("section")),
                        excerpt(doc.getText()), score.score());
                hits.add(new KnowledgeHit(doc.getText(), citation));
            }
            return hits;
        } catch (Exception e) {
            log.warn("知识检索失败，降级为无 RAG 回答: {}", e.getMessage());
            return List.of();
        }
    }

    private String excerpt(String text) {
        return text == null ? "" : text.substring(0, Math.min(text.length(), 180));
    }

    private String asString(Object value) { return value == null ? null : String.valueOf(value); }
    private boolean belongsToAgent(Document document, String agentType) {
        String documentAgentType = asString(document.getMetadata().get("agentType"));
        // Existing health vectors predate agent isolation. They remain health-only for backward compatibility.
        return agentType.equalsIgnoreCase(documentAgentType)
                || ("HEALTH".equalsIgnoreCase(agentType) && documentAgentType == null);
    }
    private Long asLong(Object value) {
        if (value instanceof Number number) return number.longValue();
        try { return value == null ? null : Long.valueOf(String.valueOf(value)); } catch (NumberFormatException e) { return null; }
    }
}
