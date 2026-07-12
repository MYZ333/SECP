package com.medcare.hda.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 积分商城商品 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("point_product")
@Schema(description = "积分商品")
public class PointProduct extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "商品名称")
    private String name;

    @Schema(description = "商品类别: 健康监测/医疗服务/康复护理/营养保健/生活家居")
    private String category;

    @Schema(description = "商品图片URL")
    private String image;

    @Schema(description = "商品描述")
    private String description;

    @Schema(description = "所需积分")
    private Integer pointsCost;

    @Schema(description = "库存")
    private Integer stock;

    @Schema(description = "状态: 0 下架 1 上架")
    private Integer status;
}
