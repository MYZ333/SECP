package com.medcare.hda.agent.knowledge;

public record KnowledgeChunkView(
        Long id, Long documentId, Integer chunkNo, String sectionTitle,
        String content, String status
) {
}
