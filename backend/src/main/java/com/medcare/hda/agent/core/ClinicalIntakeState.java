package com.medcare.hda.agent.core;

import java.util.List;

/** Persisted state for the active symptom-intake episode in a chat session. */
public record ClinicalIntakeState(
        Long userId,
        String sessionId,
        String episodeId,
        String phase,
        int roundCount,
        String initialQuestion,
        String clinicalSummary,
        List<String> knownFacts,
        List<String> missingFields
) { }
