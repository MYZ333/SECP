package com.medcare.hda.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
}
