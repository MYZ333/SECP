package com.medcare.hda.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** 健康预警 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("health_alert")
@Schema(description = "健康预警")
public class HealthAlert extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "预警级别: LOW/MEDIUM/HIGH")
    private String level;

    @Schema(description = "预警类型, 如 血压异常")
    private String alertType;

    @Schema(description = "预警内容/AI建议")
    private String content;

    @Schema(description = "是否已读: 0 未读 1 已读")
    private Integer readFlag;

    @Schema(description = "处理状态: OPEN/ACKNOWLEDGED/IN_PROGRESS/RESOLVED/IGNORED")
    private String status;

    @Schema(description = "本轮预警被异常体征触发或更新的次数")
    private Integer triggerCount;

    @Schema(description = "最近一次触发该预警的体征ID")
    private Long latestMetricId;

    @Schema(description = "最近触发时间")
    private LocalDateTime lastTriggerTime;

    @Schema(description = "处理渠道: HEALTH_ASSISTANT/DOCTOR_CONSULT")
    private String handlingChannel;

    @Schema(description = "关联的健康助手或医生咨询会话ID")
    private String relatedSessionId;

    @Schema(description = "解决或忽略时间")
    private LocalDateTime resolvedTime;

    @Schema(description = "解决说明或忽略原因")
    private String resolutionNote;

    @JsonIgnore
    @Schema(description = "生成指纹，用于避免同一批体征重复生成预警", hidden = true)
    private String generationKey;
}
