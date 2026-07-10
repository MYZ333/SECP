package com.medcare.hda.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/** 个人健康档案-健康报告 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("health_report")
@Schema(description = "健康报告")
public class HealthReport extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "所属用户ID")
    private Long userId;

    @Schema(description = "报告标题")
    private String title;

    @Schema(description = "报告类型: PHYSICAL 体检报告 / AI AI生成 / OTHER")
    private String reportType;

    @Schema(description = "报告日期")
    private LocalDate reportDate;

    @Schema(description = "报告内容/结论")
    private String content;

    @Schema(description = "附件URL")
    private String fileUrl;
}
