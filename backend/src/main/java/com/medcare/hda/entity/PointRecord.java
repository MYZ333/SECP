package com.medcare.hda.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 积分流水明细 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("point_record")
@Schema(description = "积分明细")
public class PointRecord extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "所属用户ID")
    private Long userId;

    @Schema(description = "变动积分(正数获得, 负数消耗)")
    private Integer changePoints;

    @Schema(description = "变动后余额")
    private Integer balance;

    @Schema(description = "类型: LOGIN 登录 / PROFILE 完善档案 / CHECKIN 打卡 / EXCHANGE 兑换 / ADMIN 管理员调整")
    private String type;

    @Schema(description = "描述")
    private String description;
}
