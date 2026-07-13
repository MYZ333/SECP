package com.medcare.hda.agent.knowledge;

import com.medcare.hda.agent.api.AgentCitation;

public record KnowledgeHit(String content, AgentCitation citation) {
}
