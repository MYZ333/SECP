package com.medcare.hda.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** 预警处理动作参数。 */
@Data
public class AlertActionDTO {
    @Size(max = 500, message = "处理说明不能超过500字")
    private String note;

    @Size(max = 30, message = "处理渠道过长")
    private String channel;

    @Size(max = 64, message = "会话ID过长")
    private String sessionId;
}
