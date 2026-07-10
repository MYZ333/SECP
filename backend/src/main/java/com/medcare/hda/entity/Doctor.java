package com.medcare.hda.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

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

    @Schema(description = "头像URL")
    private String avatar;

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
}
