package com.medcare.hda.agent.knowledge;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KnowledgeRetrievalServiceTest {
    @Test
    void shouldPromoteDocumentsReturnedByBothRecallChannels() {
        KnowledgeRetrievalService service = service(mock(ObjectProvider.class), mock(KnowledgeQueryRewriter.class),
                mock(KeywordKnowledgeRetriever.class), mock(DashScopeRerankClient.class));
        Document vectorOnly = document("a", "vector");
        Document shared = document("b", "shared");
        Document keywordOnly = document("c", "keyword");

        List<Document> fused = service.reciprocalRankFusion(
                List.of(vectorOnly, shared), List.of(shared, keywordOnly));

        assertEquals(List.of("b", "a", "c"), fused.stream().map(Document::getId).toList());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnKeywordEvidenceWhenVectorStoreIsUnavailable() {
        ObjectProvider<VectorStore> provider = mock(ObjectProvider.class);
        KnowledgeQueryRewriter rewriter = mock(KnowledgeQueryRewriter.class);
        KeywordKnowledgeRetriever keywordRetriever = mock(KeywordKnowledgeRetriever.class);
        DashScopeRerankClient reranker = mock(DashScopeRerankClient.class);
        Document keywordDocument = document("knowledge-7", "高血压管理资料");
        when(provider.getIfAvailable()).thenReturn(null);
        when(rewriter.rewrite(anyString(), anyString())).thenReturn(
                new KnowledgeQueryRewriter.RewrittenQuery("高血压怎么办", "高血压健康管理",
                        List.of("高血压", "血压管理"), true));
        when(keywordRetriever.search(anyList(), anyString())).thenReturn(List.of(keywordDocument));
        when(reranker.rerank(anyString(), anyList(), anyInt())).thenReturn(
                List.of(new DashScopeRerankClient.ScoredIndex(0, 0.9)));

        List<KnowledgeHit> hits = service(provider, rewriter, keywordRetriever, reranker)
                .search("高血压怎么办");

        assertEquals(1, hits.size());
        assertEquals("高血压管理资料", hits.getFirst().content());
        assertEquals(0.9, hits.getFirst().citation().score());
    }

    private KnowledgeRetrievalService service(ObjectProvider<VectorStore> provider,
                                              KnowledgeQueryRewriter rewriter,
                                              KeywordKnowledgeRetriever keywordRetriever,
                                              DashScopeRerankClient reranker) {
        return new KnowledgeRetrievalService(provider, rewriter, keywordRetriever, reranker,
                true, 30, 5, 0.35, 12, 60, 1.0, 0.8);
    }

    private Document document(String id, String text) {
        return Document.builder().id(id).text(text).metadata(Map.of(
                "documentId", 1L,
                "title", "title",
                "sourceOrg", "source",
                "sourceUrl", "https://example.test",
                "publishedDate", "2026-01-01",
                "section", "section",
                "agentType", "HEALTH")).build();
    }
}
