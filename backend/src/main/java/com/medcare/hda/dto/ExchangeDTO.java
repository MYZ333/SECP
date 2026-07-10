package com.medcare.hda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "积分兑换请求")
public class ExchangeDTO {
    @NotNull(message = "商品ID不能为空")
    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "数量")
    private Integer quantity = 1;

    @Schema(description = "收货信息")
    private String address;
}
