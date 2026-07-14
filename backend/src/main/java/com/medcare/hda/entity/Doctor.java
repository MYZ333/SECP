package com.medcare.hda.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/** 医生专家库 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doctor")
@Schema(description = "医生专家")
public class Doctor extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "医生姓名")
    private String name;

    @Schema(description = "关联登录用户ID")
    private Long userId;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "职称, 如 主任医师")
    private String title;

    @Schema(description = "所属医院")
    private String hospital;

    @Schema(description = "科室")
    private String department;

    @Schema(description = "擅长领域")
    private String speciality;

    @Schema(description = "简介")
    private String introduction;

    @Schema(description = "状态: 0 停用 1 启用")
    private Integer status;

    @Schema(description = "审核状态: PENDING / APPROVED / REJECTED")
    private String auditStatus;

    @TableField(exist = false)
    @Schema(description = "平均评分")
    private Double averageRating;

    @TableField(exist = false)
    @Schema(description = "评分总数")
    private Integer ratingCount;

    @TableField(exist = false)
    @Schema(description = "各星级评价数量，key 为 1-5")
    private Map<Integer, Integer> ratingCounts;
}
