package com.medcare.hda.agent.knowledge;

import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KnowledgeAdminServiceTest {

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void inactiveDeletesStableVectorKeysAndClearsStoredKey() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        KnowledgeDocumentParser parser = mock(KnowledgeDocumentParser.class);
        KnowledgeChunker chunker = mock(KnowledgeChunker.class);
        ObjectProvider<VectorStore> provider = mock(ObjectProvider.class);
        VectorStore vectorStore = mock(VectorStore.class);
        when(provider.getIfAvailable()).thenReturn(vectorStore);
        when(jdbcTemplate.queryForList(anyString(), eq(7L))).thenReturn(List.of(Map.of("id", 7L)));
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(7L)))
                .thenReturn(List.of("knowledge-11", "knowledge-12"));

        KnowledgeAdminService service = new KnowledgeAdminService(jdbcTemplate, parser, chunker, provider);
        service.inactive(7L);

        verify(vectorStore).delete(List.of("knowledge-11", "knowledge-12"));
        verify(jdbcTemplate).update(contains("vector_id=NULL"), eq(7L));
        verify(jdbcTemplate).update(contains("knowledge_document SET status='INACTIVE'"), eq(7L));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void deleteRemovesChunksAndDocumentWhenAlreadyInactive() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ObjectProvider<VectorStore> provider = mock(ObjectProvider.class);
        when(jdbcTemplate.queryForList(anyString(), eq(9L)))
                .thenReturn(List.of(Map.of("id", 9L, "status", "INACTIVE", "file_path", "")));

        KnowledgeAdminService service = new KnowledgeAdminService(jdbcTemplate,
                mock(KnowledgeDocumentParser.class), mock(KnowledgeChunker.class), provider);
        service.delete(9L);

        verify(jdbcTemplate).update(contains("DELETE FROM knowledge_chunk"), eq(9L));
        verify(jdbcTemplate).update(contains("DELETE FROM knowledge_document"), eq(9L));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void deleteRejectsDocumentThatIsNotInactive() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ObjectProvider<VectorStore> provider = mock(ObjectProvider.class);
        when(jdbcTemplate.queryForList(anyString(), eq(10L)))
                .thenReturn(List.of(Map.of("id", 10L, "status", "PUBLISHED")));
        KnowledgeAdminService service = new KnowledgeAdminService(jdbcTemplate,
                mock(KnowledgeDocumentParser.class), mock(KnowledgeChunker.class), provider);

        assertThrows(com.medcare.hda.exception.BusinessException.class, () -> service.delete(10L));

        verify(jdbcTemplate, never()).update(contains("DELETE FROM knowledge_chunk"), eq(10L));
        verify(jdbcTemplate, never()).update(contains("DELETE FROM knowledge_document"), eq(10L));
    }
}
