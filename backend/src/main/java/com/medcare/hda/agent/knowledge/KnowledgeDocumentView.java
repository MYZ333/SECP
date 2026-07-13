package com.medcare.hda.agent.knowledge;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record KnowledgeDocumentView(
        Long id, String agentType, String title, String sourceOrg, String sourceUrl,
        LocalDate publishedDate, String versionNo, String category,
        String fileName, String status, Integer chunkCount,
        String failureReason, LocalDateTime createTime, LocalDateTime updateTime
) {
}
