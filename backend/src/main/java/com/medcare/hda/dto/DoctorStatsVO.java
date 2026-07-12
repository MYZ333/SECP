package com.medcare.hda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "医生工作台统计")
public class DoctorStatsVO {
    private long patientCount;
    private long openSessionCount;
    private long unreadCount;
    private long todayMessageCount;
}
