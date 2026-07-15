package com.medcare.hda.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

    @Schema(description = "统计周期开始时间")
    private LocalDateTime periodStart;

    @Schema(description = "统计周期结束时间")
    private LocalDateTime periodEnd;

    @Schema(description = "综合风险等级: INSUFFICIENT/NORMAL/ATTENTION/WARNING/HIGH")
    private String riskLevel;

    @Schema(description = "生成算法版本")
    private String algorithmVersion;

    @Schema(description = "生成模式: RULE/RULE_AI")
    private String generationMode;

    @Schema(description = "有效数据条数")
    private Integer dataCount;

    @Schema(description = "数据质量, 0-1")
    private Double dataQuality;

    @Schema(description = "结构化报告JSON快照")
    private String structuredResult;
}
