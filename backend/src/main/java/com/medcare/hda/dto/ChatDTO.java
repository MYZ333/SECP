package com.medcare.hda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "健康咨询请求")
public class ChatDTO {
    @NotBlank(message = "咨询内容不能为空")
    @Schema(description = "用户提问")
    private String message;

    @Schema(description = "会话ID(可选, 用于多轮)")
    private String sessionId;
}
