package com.medcare.hda.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** 个人健康档案-体征/体检数据（时间序列） */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("health_metric")
@Schema(description = "体征/体检数据")
public class HealthMetric extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "所属用户ID")
    private Long userId;

    @Schema(description = "指标类型: BLOOD_PRESSURE/BLOOD_SUGAR/HEART_RATE/TEMPERATURE/WEIGHT 等")
    private String metricType;

    @Schema(description = "指标值(数值)")
    private Double metricValue;

    @Schema(description = "第二数值(如血压舒张压)")
    private Double metricValue2;

    @Schema(description = "单位, 如 mmHg / mmol/L / 次/分 / ℃")
    private String unit;

    @Schema(description = "测量时间")
    private LocalDateTime measureTime;

    @Schema(description = "是否异常: 0 正常 1 异常")
    private Integer abnormal;

    @Schema(description = "备注")
    private String remark;
}
