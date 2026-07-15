package com.medcare.hda.agent.config;

import com.medcare.hda.agent.memory.PersistentWindowChatMemory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/** 健康助手的 ChatClient 与持久化对话记忆配置。 */
@Configuration
public class HealthAssistantAgentConfig {

    private static final int MEMORY_WINDOW_SIZE = 20;

    @Bean
    public ChatMemory healthAssistantChatMemory(ChatMemoryRepository chatMemoryRepository) {
        return new PersistentWindowChatMemory(chatMemoryRepository, MEMORY_WINDOW_SIZE);
    }

    @Bean
    public ChatClient healthAssistantChatClient(ChatClient.Builder chatClientBuilder) {
        return chatClientBuilder
                .clone()
                .build();
    }

    /** 报告文案专用客户端：无记忆、无 RAG，并使用低温度减少自由发挥。 */
    @Bean
    public ChatClient healthReportChatClient(ChatClient.Builder chatClientBuilder) {
        return chatClientBuilder
                .clone()
                .defaultOptions(ChatOptions.builder().temperature(0.2).build())
                .build();
    }

    @Bean(name = "healthAgentExecutor")
    public Executor healthAgentExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("health-agent-");
        executor.initialize();
        return executor;
    }
}
