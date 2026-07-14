package com.medcare.hda.agent.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.ai.chat.client.ChatClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KnowledgeQueryRewriterTest {
    @Test
    void shouldUseRuleRewriteWhenModelIsUnavailable() {
        ChatClient chatClient = mock(ChatClient.class);
        KnowledgeQueryRewriter rewriter = new KnowledgeQueryRewriter(
                chatClient, new ObjectMapper(), true, "sk-placeholder", 6);

        KnowledgeQueryRewriter.RewrittenQuery result = rewriter.rewrite("高血压应该怎么管理？", "HEALTH");

        assertFalse(result.modelRewritten());
        assertEquals("高血压应该怎么管理？", result.originalQuery());
        assertTrue(result.semanticQuery().contains("健康科普"));
        assertTrue(result.keywords().contains("高血压应该怎么管理"));
    }

    @Test
    void shouldParseModelRewriteAndKeepFallbackKeywords() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec request = mock(ChatClient.ChatClientRequestSpec.class, Answers.RETURNS_SELF);
        ChatClient.CallResponseSpec response = mock(ChatClient.CallResponseSpec.class);
        when(chatClient.prompt()).thenReturn(request);
        when(request.call()).thenReturn(response);
        when(response.content()).thenReturn("```json\n{\"query\":\"老年高血压日常管理建议\","
                + "\"keywords\":[\"老年高血压\",\"血压管理\"]}\n```");
        KnowledgeQueryRewriter rewriter = new KnowledgeQueryRewriter(
                chatClient, new ObjectMapper(), true, "sk-real-key", 6);

        KnowledgeQueryRewriter.RewrittenQuery result = rewriter.rewrite("老人血压高怎么办", "HEALTH");

        assertTrue(result.modelRewritten());
        assertEquals("老年高血压日常管理建议", result.semanticQuery());
        assertEquals("老年高血压", result.keywords().getFirst());
        assertTrue(result.keywords().contains("老人血压高怎么办"));
    }
}
