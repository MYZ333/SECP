package com.medcare.hda.agent.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "健康助手推荐医生")
public record DoctorRecommendation(
        Long doctorId,
        String name,
        String avatar,
        String title,
        String hospital,
        String department,
        String speciality,
        String introduction,
        @Schema(description = "0-100 诉求匹配度，不代表医疗质量") int matchScore,
        List<String> reasons,
        DoctorRecommendationAction action
) {
}
