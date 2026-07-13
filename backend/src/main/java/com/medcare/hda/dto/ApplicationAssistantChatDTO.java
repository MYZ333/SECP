package com.medcare.hda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "应用使用助手请求")
public class ApplicationAssistantChatDTO {
    @NotBlank(message = "提问内容不能为空")
    @Schema(description = "用户关于应用使用方式的提问")
    private String message;
}
