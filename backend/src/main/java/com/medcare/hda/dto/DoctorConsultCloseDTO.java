package com.medcare.hda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "医生结束咨询请求")
public class DoctorConsultCloseDTO {

    @Schema(description = "本次咨询问题概述")
    @Size(max = 1000, message = "问题概述不能超过1000字")
    private String problemOverview;

    @Schema(description = "医生初步判断")
    @Size(max = 1000, message = "初步判断不能超过1000字")
    private String preliminaryAssessment;

    @Schema(description = "本次咨询总结")
    @Size(max = 1000, message = "咨询总结不能超过1000字")
    private String summary;

    @Schema(description = "后续建议")
    @Size(max = 1000, message = "后续建议不能超过1000字")
    private String advice;

    @Schema(description = "风险提醒")
    @Size(max = 1000, message = "风险提醒不能超过1000字")
    private String riskWarning;

    @Schema(description = "是否建议线下就医")
    private Boolean recommendOffline;
}
