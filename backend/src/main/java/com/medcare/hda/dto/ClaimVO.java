package com.medcare.hda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "领取积分结果")
public class ClaimVO {
    @Schema(description = "本次领取积分")
    private Integer points;
    @Schema(description = "领取后积分余额")
    private Integer balance;
}
