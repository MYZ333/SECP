package com.medcare.hda.agent.memory;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersistentWindowChatMemoryTest {

    @Test
    void shouldKeepFullHistoryWhileReturningOnlyRecentWindow() {
        InMemoryRepository repository = new InMemoryRepository();
        PersistentWindowChatMemory memory = new PersistentWindowChatMemory(repository, 2);

        memory.add("conversation", List.of(new UserMessage("one")));
        memory.add("conversation", List.of(new UserMessage("two")));
        memory.add("conversation", List.of(new UserMessage("three")));

        assertEquals(3, repository.findByConversationId("conversation").size());
        assertEquals(List.of("two", "three"), memory.get("conversation").stream().map(Message::getText).toList());
    }

    private static final class InMemoryRepository implements ChatMemoryRepository {
        private final Map<String, List<Message>> messages = new HashMap<>();

        @Override
        public List<String> findConversationIds() {
            return new ArrayList<>(messages.keySet());
        }

        @Override
        public List<Message> findByConversationId(String conversationId) {
            return new ArrayList<>(messages.getOrDefault(conversationId, List.of()));
        }

        @Override
        public void saveAll(String conversationId, List<Message> conversationMessages) {
            messages.put(conversationId, new ArrayList<>(conversationMessages));
        }

        @Override
        public void deleteByConversationId(String conversationId) {
            messages.remove(conversationId);
        }
    }
}
