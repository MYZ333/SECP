package com.medcare.hda.agent.core;

import com.medcare.hda.agent.api.AgentIntakeQuestion;

import java.util.List;

public record ClinicalIntakeAssessment(
        Decision decision,
        String clinicalSummary,
        List<String> knownFacts,
        List<String> missingFields,
        AgentIntakeQuestion question,
        boolean newEpisode,
        boolean insufficient
) {
    public enum Decision { ASK, READY, DIRECT_EDUCATION }
}
