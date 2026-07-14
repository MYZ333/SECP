package com.medcare.hda.agent.core;

import com.medcare.hda.agent.api.AgentStreamEvent;
import com.medcare.hda.agent.knowledge.KnowledgeRetrievalService;
import com.medcare.hda.agent.memory.LongTermMemoryService;
import com.medcare.hda.agent.memory.MemorySourceAgent;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApplicationAssistantServiceTest {

    @Test
    void shouldStreamModelDeltasWithApplicationPromptAndFinishWithDone() {
        ChatClient chatClient = mock(ChatClient.class);
        KnowledgeRetrievalService retrievalService = mock(KnowledgeRetrievalService.class);
        ChatClient.ChatClientRequestSpec request = mock(ChatClient.ChatClientRequestSpec.class, Answers.RETURNS_SELF);
        ChatClient.StreamResponseSpec streamResponse = mock(ChatClient.StreamResponseSpec.class);
        when(chatClient.prompt()).thenReturn(request);
        when(retrievalService.search(anyString(), anyString())).thenReturn(List.of());
        when(request.stream()).thenReturn(streamResponse);
        when(streamResponse.content()).thenReturn(Flux.just("first", "second"));

        List<AgentStreamEvent> events = new ApplicationAssistantService(chatClient, retrievalService)
                .stream("where is the health profile")
                .collectList()
                .block();

        verify(request).system(contains("应用使用助手"));
        verify(request).user(anyString());
        verify(request).stream();
        assertEquals(List.of("delta", "delta", "done"), events.stream().map(AgentStreamEvent::type).toList());
        assertEquals("firstsecond", events.stream()
                .filter(event -> "delta".equals(event.type()))
                .map(AgentStreamEvent::content)
                .reduce("", String::concat));
    }

    @Test
    void shouldUseOnlyApplicationVisibleMemoryAndEnqueueCompletedTurn() {
        ChatClient chatClient = mock(ChatClient.class);
        KnowledgeRetrievalService retrievalService = mock(KnowledgeRetrievalService.class);
        LongTermMemoryService memoryService = mock(LongTermMemoryService.class);
        ChatClient.ChatClientRequestSpec request = mock(ChatClient.ChatClientRequestSpec.class, Answers.RETURNS_SELF);
        ChatClient.StreamResponseSpec streamResponse = mock(ChatClient.StreamResponseSpec.class);
        when(chatClient.prompt()).thenReturn(request);
        when(retrievalService.search(anyString(), anyString())).thenReturn(List.of());
        when(memoryService.promptContext(7L, "how to use it", MemorySourceAgent.APPLICATION))
                .thenReturn("\nshared preference");
        when(request.stream()).thenReturn(streamResponse);
        when(streamResponse.content()).thenReturn(Flux.just("answer"));
        ApplicationAssistantService service = new ApplicationAssistantService(chatClient, retrievalService);
        ReflectionTestUtils.setField(service, "longTermMemoryService", memoryService);

        service.stream(7L, "how to use it").collectList().block();

        verify(request).system(contains("shared preference"));
        verify(memoryService).enqueueTurn(7L, MemorySourceAgent.APPLICATION, null, null,
                "how to use it", "answer");
    }
}
