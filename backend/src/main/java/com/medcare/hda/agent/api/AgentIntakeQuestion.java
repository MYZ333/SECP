package com.medcare.hda.agent.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "健康助手逐步问诊题目")
public record AgentIntakeQuestion(
        @Schema(description = "问题文本") String prompt,
        @Schema(description = "可直接选择的答案") List<String> options,
        @Schema(description = "是否允许自由回答") boolean allowFreeText
) { }
