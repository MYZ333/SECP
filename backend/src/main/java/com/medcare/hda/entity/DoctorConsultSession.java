package com.medcare.hda.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** 医生咨询会话 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doctor_consult_session")
@Schema(description = "医生咨询会话")
public class DoctorConsultSession extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "患者用户ID")
    private Long userId;

    @Schema(description = "医生ID")
    private Long doctorId;

    @Schema(description = "状态: OPEN / CLOSED")
    private String status;

    @Schema(description = "最后一条消息")
    private String lastMessage;

    @Schema(description = "最后消息时间")
    private LocalDateTime lastMessageTime;

    @Schema(description = "患者侧未读数")
    private Integer unreadUser;

    @Schema(description = "医生侧未读数")
    private Integer unreadDoctor;
}
