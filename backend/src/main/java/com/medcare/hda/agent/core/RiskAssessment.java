package com.medcare.hda.agent.core;

public record RiskAssessment(String level, String message, boolean emergency) {
}
