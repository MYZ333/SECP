package com.medcare.hda.agent.core;

import com.medcare.hda.agent.api.AgentCitation;
import com.medcare.hda.agent.api.AgentIntakeQuestion;
import com.medcare.hda.agent.api.AgentStageUpdate;
import com.medcare.hda.agent.api.DoctorRecommendation;

import java.util.List;

public record PreparedAgentResponse(
        String traceId,
        RiskAssessment risk,
        List<AgentCitation> citations,
        List<String> usedProfileCategories,
        List<AgentStageUpdate> stages,
        List<DoctorRecommendation> recommendedDoctors,
        String route,
        String systemPrompt,
        String directContent,
        AgentIntakeQuestion intakeQuestion
) {
    public boolean direct() { return directContent != null; }
}
