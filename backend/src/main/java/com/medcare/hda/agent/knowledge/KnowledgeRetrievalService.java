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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class KnowledgeRetrievalService {
    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final KnowledgeQueryRewriter queryRewriter;
    private final KeywordKnowledgeRetriever keywordRetriever;
    private final DashScopeRerankClient reranker;
    private final boolean enabled;
    private final int vectorTopK;
    private final int finalTopK;
    private final double minScore;
    private final int rerankTopK;
    private final int rrfK;
    private final double vectorWeight;
    private final double keywordWeight;

    public KnowledgeRetrievalService(ObjectProvider<VectorStore> vectorStoreProvider,
                                     KnowledgeQueryRewriter queryRewriter,
                                     KeywordKnowledgeRetriever keywordRetriever,
                                     DashScopeRerankClient reranker,
                                     @Value("${hda.agent.rag.enabled:true}") boolean enabled,
                                     @Value("${hda.agent.rag.vector-top-k:30}") int vectorTopK,
                                     @Value("${hda.agent.rag.final-top-k:5}") int finalTopK,
                                     @Value("${hda.agent.rag.min-score:0.35}") double minScore,
                                     @Value("${hda.agent.rag.hybrid.rerank-top-k:12}") int rerankTopK,
                                     @Value("${hda.agent.rag.hybrid.rrf-k:60}") int rrfK,
                                     @Value("${hda.agent.rag.hybrid.vector-weight:1.0}") double vectorWeight,
                                     @Value("${hda.agent.rag.hybrid.keyword-weight:0.8}") double keywordWeight) {
        this.vectorStoreProvider = vectorStoreProvider;
        this.queryRewriter = queryRewriter;
        this.keywordRetriever = keywordRetriever;
        this.reranker = reranker;
        this.enabled = enabled;
        this.vectorTopK = vectorTopK;
        this.finalTopK = finalTopK;
        this.minScore = minScore;
        this.rerankTopK = Math.max(finalTopK, rerankTopK);
        this.rrfK = Math.max(1, rrfK);
        this.vectorWeight = Math.max(0D, vectorWeight);
        this.keywordWeight = Math.max(0D, keywordWeight);
    }

    public List<KnowledgeHit> search(String query) {
        return search(query, "HEALTH");
    }

    public List<KnowledgeHit> search(String query, String agentType) {
        if (!enabled) return List.of();
        KnowledgeQueryRewriter.RewrittenQuery rewritten = queryRewriter.rewrite(query, agentType);
        try {
            List<Document> vectorCandidates = vectorSearch(
                    vectorStoreProvider.getIfAvailable(), rewritten.semanticQuery(), agentType);
            List<Document> keywordCandidates = keywordSearch(rewritten.keywords(), agentType);
            List<Document> candidates = reciprocalRankFusion(vectorCandidates, keywordCandidates);
            if (candidates.isEmpty()) return List.of();

            List<String> texts = candidates.stream().map(Document::getText).toList();
            List<DashScopeRerankClient.ScoredIndex> scored = reranker.rerank(
                    rewritten.originalQuery(), texts, Math.min(rerankTopK, candidates.size()));
            List<KnowledgeHit> hits = new ArrayList<>();
            for (DashScopeRerankClient.ScoredIndex score : scored) {
                if (hits.size() >= finalTopK) break;
                if (score.index() < 0 || score.index() >= candidates.size() || score.score() < minScore) continue;
                Document doc = candidates.get(score.index());
                Map<String, Object> meta = doc.getMetadata();
                AgentCitation citation = new AgentCitation(
                        asLong(meta.get("documentId")), asString(meta.get("title")), asString(meta.get("sourceOrg")),
                        asString(meta.get("sourceUrl")), asString(meta.get("publishedDate")), asString(meta.get("section")),
                        excerpt(doc.getText()), score.score());
                hits.add(new KnowledgeHit(doc.getText(), citation));
            }
            return hits;
        } catch (Exception error) {
            log.warn("知识检索失败，降级为无 RAG 回答: {}", error.getMessage());
            return List.of();
        }
    }

    private List<Document> vectorSearch(VectorStore vectorStore, String query, String agentType) {
        if (vectorStore == null) {
            log.warn("RAG 已启用但没有可用的 VectorStore，降级为关键词检索");
            return List.of();
        }
        try {
            List<Document> candidates = vectorStore.similaritySearch(SearchRequest.builder()
                    .query(query).topK(vectorTopK).build());
            if (candidates == null) return List.of();
            return candidates.stream().filter(document -> belongsToAgent(document, agentType)).toList();
        } catch (Exception error) {
            log.warn("向量召回不可用，降级为关键词检索: {}", error.getMessage());
            return List.of();
        }
    }

    private List<Document> keywordSearch(List<String> keywords, String agentType) {
        try {
            return keywordRetriever.search(keywords, agentType);
        } catch (Exception error) {
            log.warn("关键词召回不可用，降级为向量检索: {}", error.getMessage());
            return List.of();
        }
    }

    /** Weighted reciprocal-rank fusion is independent of the score scales of the two recall channels. */
    List<Document> reciprocalRankFusion(List<Document> vectorResults, List<Document> keywordResults) {
        Map<String, Document> documents = new LinkedHashMap<>();
        Map<String, Double> scores = new HashMap<>();
        addRankedResults(vectorResults, vectorWeight, documents, scores);
        addRankedResults(keywordResults, keywordWeight, documents, scores);
        return documents.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, Document>>comparingDouble(entry -> scores.get(entry.getKey()))
                        .reversed())
                .map(Map.Entry::getValue)
                .toList();
    }

    private void addRankedResults(List<Document> results, double weight, Map<String, Document> documents,
                                  Map<String, Double> scores) {
        if (results == null || weight <= 0D) return;
        for (int index = 0; index < results.size(); index++) {
            Document document = results.get(index);
            String key = document.getId();
            documents.putIfAbsent(key, document);
            scores.merge(key, weight / (rrfK + index + 1D), Double::sum);
        }
    }

    private String excerpt(String text) {
        return text == null ? "" : text.substring(0, Math.min(text.length(), 180));
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private boolean belongsToAgent(Document document, String agentType) {
        String documentAgentType = asString(document.getMetadata().get("agentType"));
        return agentType.equalsIgnoreCase(documentAgentType)
                || ("HEALTH".equalsIgnoreCase(agentType) && documentAgentType == null);
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) return number.longValue();
        try {
            return value == null ? null : Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException error) {
            return null;
        }
    }
}
