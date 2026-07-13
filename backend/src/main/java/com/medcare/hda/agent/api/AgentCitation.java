package com.medcare.hda.agent.api;

import io.swagger.v3.oas.annotations.media.Schema;

/** 用户可验证的知识来源。 */
@Schema(description = "健康知识来源引用")
public record AgentCitation(
        Long documentId,
        String title,
        String sourceOrg,
        String sourceUrl,
        String publishedDate,
        String section,
        String excerpt,
        Double score
) {
}
