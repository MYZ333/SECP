package com.medcare.hda.agent.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "医生推荐卡片动作")
public record DoctorRecommendationAction(
        @Schema(description = "动作类型") String type,
        @Schema(description = "前端路由") String route,
        @Schema(description = "医生 ID") Long doctorId
) {
}
