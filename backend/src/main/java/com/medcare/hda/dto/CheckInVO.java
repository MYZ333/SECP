package com.medcare.hda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/** 签到结果 */
@Data
@Builder
@Schema(description = "签到结果")
public class CheckInVO {

    @Schema(description = "本次获得积分")
    private Integer points;

    @Schema(description = "连续签到天数")
    private Integer streakDays;

    @Schema(description = "当前积分余额")
    private Integer balance;
}
