package com.medcare.hda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/** 积分任务(获取方式)状态 */
@Data
@Builder
@Schema(description = "积分任务")
public class PointTaskVO {

    @Schema(description = "任务类型: CHECKIN/LOGIN/METRIC/CONSULT/PROFILE")
    private String type;

    @Schema(description = "任务名称")
    private String name;

    @Schema(description = "任务说明")
    private String description;

    @Schema(description = "可得积分(签到为今日可得, 含连签加成)")
    private Integer points;

    @Schema(description = "是否每日任务(false 为一次性)")
    private Boolean daily;

    @Schema(description = "今日/已经 是否完成(=已领取)")
    private Boolean done;

    @Schema(description = "任务状态: TODO 去完成 / CLAIMABLE 待领取 / DONE 已完成")
    private String status;

    @Schema(description = "连续签到天数(仅签到任务)")
    private Integer streakDays;
}
