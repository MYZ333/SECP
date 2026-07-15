package com.medcare.hda.agent.doctor;

import java.util.List;

public record DoctorRecommendationCriteria(
        List<String> targetDepartments,
        List<String> specialtyKeywords,
        String preferredHospital
) {
    public DoctorRecommendationCriteria {
        targetDepartments = targetDepartments == null ? List.of() : List.copyOf(targetDepartments);
        specialtyKeywords = specialtyKeywords == null ? List.of() : List.copyOf(specialtyKeywords);
        preferredHospital = preferredHospital == null ? "" : preferredHospital.trim();
    }
}
