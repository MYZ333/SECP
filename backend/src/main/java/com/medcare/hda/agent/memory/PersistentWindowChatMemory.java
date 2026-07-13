package com.medcare.hda.agent.memory;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 完整消息持久化到 ChatMemoryRepository，而只向模型提供最近的消息窗口。
 * 这样历史查询不会因上下文窗口截断而丢失旧消息。
 */
public final class PersistentWindowChatMemory implements ChatMemory {

    private final ChatMemoryRepository repository;
    private final int maxMessages;
    private final ConcurrentMap<String, Object> conversationLocks = new ConcurrentHashMap<>();

    public PersistentWindowChatMemory(ChatMemoryRepository repository, int maxMessages) {
        if (maxMessages < 1) {
            throw new IllegalArgumentException("maxMessages 必须大于 0");
        }
        this.repository = repository;
        this.maxMessages = maxMessages;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        synchronized (conversationLocks.computeIfAbsent(conversationId, ignored -> new Object())) {
            List<Message> allMessages = new ArrayList<>(repository.findByConversationId(conversationId));
            allMessages.addAll(messages);
            repository.saveAll(conversationId, allMessages);
        }
    }

    @Override
    public List<Message> get(String conversationId) {
        List<Message> allMessages = repository.findByConversationId(conversationId);
        int start = Math.max(0, allMessages.size() - maxMessages);
        return new ArrayList<>(allMessages.subList(start, allMessages.size()));
    }

    @Override
    public void clear(String conversationId) {
        repository.deleteByConversationId(conversationId);
        conversationLocks.remove(conversationId);
    }
}
