package com.medcare.hda.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 医生咨询消息 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doctor_consult_message")
@Schema(description = "医生咨询消息")
public class DoctorConsultMessage extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "会话ID")
    private Long sessionId;

    @Schema(description = "患者用户ID")
    private Long userId;

    @Schema(description = "医生ID")
    private Long doctorId;

    @Schema(description = "发送方: USER / DOCTOR")
    private String senderType;

    @Schema(description = "消息类型: TEXT / ATTACHMENT")
    private String messageType;

    @Schema(description = "文本内容")
    private String content;

    @Schema(description = "附件URL")
    private String attachmentUrl;

    @Schema(description = "附件名称")
    private String attachmentName;

    @Schema(description = "是否已读: 0 未读 1 已读")
    private Integer readFlag;
}
