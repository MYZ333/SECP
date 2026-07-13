package com.medcare.hda.agent.knowledge;

import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KnowledgeChunkerTest {
    @Test
    void shouldKeepHeadingMetadataAndSplitLongChineseText() {
        String text = "# 合理用药\n" + "应遵循医生或药师指导。".repeat(90) + "\n# 科学就医\n出现紧急情况应及时就医。";
        ObjectProvider<EmbeddingModel> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        List<KnowledgeChunker.Chunk> chunks = new KnowledgeChunker(provider,
                true, 280, 1100, 0.80, 0.12).chunk(text);
        assertTrue(chunks.size() >= 3);
        assertEquals("合理用药", chunks.getFirst().section());
        assertEquals("科学就医", chunks.getLast().section());
        assertTrue(chunks.stream().allMatch(chunk -> !chunk.content().isBlank()));
    }

    @Test
    void shouldSplitAtEmbeddingSemanticTransition() {
        ObjectProvider<EmbeddingModel> provider = mock(ObjectProvider.class);
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        when(provider.getIfAvailable()).thenReturn(embeddingModel);
        when(embeddingModel.embed(anyList())).thenAnswer(invocation -> {
            List<String> texts = invocation.getArgument(0);
            return java.util.stream.IntStream.range(0, texts.size())
                    .mapToObj(index -> index < 4 ? new float[]{1F, 0F} : new float[]{0F, 1F})
                    .toList();
        });

        String exercise = "规律运动能够帮助控制体重并改善心肺功能，需要结合个人能力循序渐进，同时记录运动后的身体感受并合理安排休息。";
        String medication = "使用处方药应遵循医生或药师指导，不应自行增加剂量或者突然停止用药，同时应主动说明过敏史和正在使用的其他药物。";
        String text = "# 健康管理\n" + exercise.repeat(4) + medication.repeat(4);
        List<KnowledgeChunker.Chunk> chunks = new KnowledgeChunker(provider,
                true, 160, 1100, 0.80, 0.12).chunk(text);

        assertEquals(2, chunks.size());
        assertTrue(chunks.getFirst().content().contains("规律运动"));
        assertTrue(chunks.getLast().content().contains("处方药"));
        assertEquals("健康管理", chunks.getFirst().section());
    }
}
