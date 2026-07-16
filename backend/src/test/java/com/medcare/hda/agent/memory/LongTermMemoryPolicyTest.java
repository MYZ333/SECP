package com.medcare.hda.agent.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LongTermMemoryPolicyTest {

    private JdbcTemplate jdbcTemplate;
    private ObjectProvider<VectorStore> vectorStoreProvider;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        vectorStoreProvider = mock(ObjectProvider.class);
    }

    @Test
    void healthFactsMustAlwaysRemainPrivate() {
        assertEquals(MemoryVisibility.HEALTH_PRIVATE,
                LongTermMemoryService.normalizeVisibility(MemoryCategory.HEALTH_FACT, MemoryVisibility.SHARED));
    }

    @Test
    void nonHealthMemoryKeepsRequestedVisibility() {
        assertEquals(MemoryVisibility.SHARED,
                LongTermMemoryService.normalizeVisibility(MemoryCategory.PREFERENCE, MemoryVisibility.SHARED));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void fallsBackToRecentMysqlMemoriesWhenVectorStoreIsUnavailable() {
        MemoryView rememberedTeam = new MemoryView("m1", "用户曾效力于斯特兰蒂斯蒙德队",
                MemoryCategory.PERSONAL_PROFILE, MemoryVisibility.SHARED, MemorySourceAgent.HEALTH,
                0.98, 1, LocalDateTime.now(), LocalDateTime.now());
        when(vectorStoreProvider.getIfAvailable()).thenReturn(null);
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), any(Object[].class)))
                .thenReturn((List) List.of(rememberedTeam));

        LongTermMemoryService service = new LongTermMemoryService(jdbcTemplate, new ObjectMapper(),
                mock(ChatClient.class), vectorStoreProvider, mock(RedissonClient.class), Runnable::run,
                true, 12, 6, 3);

        assertEquals(List.of(rememberedTeam),
                service.search(7L, "你还记得我来自哪只篮球队吗", MemorySourceAgent.HEALTH, 6));
    }
}
