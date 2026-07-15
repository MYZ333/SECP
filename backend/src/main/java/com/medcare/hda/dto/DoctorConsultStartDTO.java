package com.medcare.hda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "创建医生咨询会话请求")
public class DoctorConsultStartDTO {
    @Schema(description = "需要转交给医生的健康助手会话 ID")
    private String healthAssistantSessionId;
}
