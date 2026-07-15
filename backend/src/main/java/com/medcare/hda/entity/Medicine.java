package com.medcare.hda.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 药品库 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("medicine")
@Schema(description = "药品")
public class Medicine extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "药品名称")
    private String name;

    @Schema(description = "通用名")
    private String genericName;

    @Schema(description = "商品名")
    private String brandName;

    @Schema(description = "药品分类")
    private String category;

    @Schema(description = "剂型")
    private String dosageForm;

    @Schema(description = "规格")
    private String specification;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "常用用法")
    private String defaultUsage;

    @Schema(description = "常用剂量")
    private String defaultDosage;

    @Schema(description = "常用频次")
    private String defaultFrequency;

    @Schema(description = "建议疗程天数")
    private Integer defaultDurationDays;

    @Schema(description = "最大建议天数")
    private Integer maxDurationDays;

    @Schema(description = "适应症说明")
    private String indications;

    @Schema(description = "禁忌症")
    private String contraindications;

    @Schema(description = "注意事项")
    private String precautions;

    @Schema(description = "不良反应")
    private String adverseReactions;

    @Schema(description = "是否需要线下就医或处方资质")
    private Integer requiresOffline;

    @Schema(description = "状态: 0 停用 1 启用")
    private Integer status;
}
