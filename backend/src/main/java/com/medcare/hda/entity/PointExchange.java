package com.medcare.hda.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 积分兑换记录 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("point_exchange")
@Schema(description = "积分兑换记录")
public class PointExchange extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "商品名称(冗余快照)")
    private String productName;

    @Schema(description = "消耗积分")
    private Integer pointsCost;

    @Schema(description = "兑换数量")
    private Integer quantity;

    @Schema(description = "状态: 0 待发货 1 已发货 2 已完成 3 已取消")
    private Integer status;

    @Schema(description = "收货信息")
    private String address;
}
