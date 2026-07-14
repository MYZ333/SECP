package com.medcare.hda.agent.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentConversationRepositoryTest {

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void shouldDeleteSessionsBeyondNewestFortyAfterTouch() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AgentConversationRepository repository = new AgentConversationRepository(jdbcTemplate, mock(ObjectMapper.class));
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString("session_id")).thenReturn("old-session");
        when(resultSet.getString("conversation_id")).thenReturn("old-conversation");
        when(jdbcTemplate.query(contains("OFFSET ?"), any(RowMapper.class), eq(7L), eq(40)))
                .thenAnswer(invocation -> {
                    RowMapper mapper = invocation.getArgument(1);
                    return List.of(mapper.mapRow(resultSet, 0));
                });

        repository.touch(7L, "current-session");

        verify(jdbcTemplate).update(
                "UPDATE agent_chat_session SET update_time = NOW() WHERE user_id = ? AND session_id = ?",
                7L, "current-session");
        verify(jdbcTemplate).update("DELETE FROM agent_consult_state WHERE user_id = ? AND session_id = ?",
                7L, "old-session");
        verify(jdbcTemplate).update("DELETE FROM agent_chat_turn WHERE user_id = ? AND session_id = ?",
                7L, "old-session");
        verify(jdbcTemplate).update("DELETE FROM SPRING_AI_CHAT_MEMORY WHERE conversation_id = ?",
                "old-conversation");
        verify(jdbcTemplate).update("DELETE FROM agent_chat_session WHERE user_id = ? AND session_id = ?",
                7L, "old-session");
    }
}
