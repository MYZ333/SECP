package com.medcare.hda.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 健康咨询对话记录（AI） */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("consult_record")
@Schema(description = "健康咨询记录")
public class ConsultRecord extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "会话ID")
    private String sessionId;

    @Schema(description = "角色: user / assistant")
    private String role;

    @Schema(description = "消息内容")
    private String content;
}
