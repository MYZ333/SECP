package com.medcare.hda.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medcare.hda.common.PageResult;
import com.medcare.hda.common.Result;
import com.medcare.hda.common.ResultCode;
import com.medcare.hda.dto.DoctorConsultFeedbackDTO;
import com.medcare.hda.dto.DoctorConsultMessageDTO;
import com.medcare.hda.dto.DoctorConsultStartDTO;
import com.medcare.hda.dto.DoctorConsultSessionVO;
import com.medcare.hda.agent.core.HealthConsultHandoffService;
import com.medcare.hda.entity.Doctor;
import com.medcare.hda.entity.DoctorConsultMessage;
import com.medcare.hda.entity.DoctorConsultSession;
import com.medcare.hda.entity.User;
import com.medcare.hda.exception.BusinessException;
import com.medcare.hda.security.SecurityUtil;
import com.medcare.hda.service.DoctorConsultMessageService;
import com.medcare.hda.service.DoctorConsultSessionService;
import com.medcare.hda.service.DoctorService;
import com.medcare.hda.service.UserService;
import com.medcare.hda.websocket.DoctorConsultNotifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "患者端-医生咨询", description = "患者选择医生并实时咨询")
@RestController
@RequestMapping("/api/doctor-consult")
@RequiredArgsConstructor
public class DoctorConsultController {

    private final DoctorService doctorService;
    private final UserService userService;
    private final DoctorConsultSessionService sessionService;
    private final DoctorConsultMessageService messageService;
    private final DoctorConsultNotifier notifier;
    private final HealthConsultHandoffService handoffService;

    @Operation(summary = "按医生创建或进入会话")
    @PostMapping("/session/{doctorId}")
    public Result<DoctorConsultSessionVO> start(@PathVariable Long doctorId,
                                                @RequestBody(required = false) DoctorConsultStartDTO dto) {
        Long userId = SecurityUtil.getUserId();
        Doctor doctor = doctorService.getById(doctorId);
        if (doctor == null || doctor.getStatus() == null || doctor.getStatus() != 1) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "医生不存在或暂不可咨询");
        }
        DoctorConsultSession session = sessionService.getOne(Wrappers.<DoctorConsultSession>lambdaQuery()
                .eq(DoctorConsultSession::getUserId, userId)
                .eq(DoctorConsultSession::getDoctorId, doctorId)
                .eq(DoctorConsultSession::getStatus, "OPEN")
                .orderByDesc(DoctorConsultSession::getLastMessageTime)
                .orderByDesc(DoctorConsultSession::getId)
                .last("LIMIT 1"));
        if (session == null) {
            session = new DoctorConsultSession();
            session.setUserId(userId);
            session.setDoctorId(doctorId);
            session.setStatus("OPEN");
            session.setUnreadDoctor(0);
            session.setUnreadUser(0);
            session.setLastMessageTime(LocalDateTime.now());
            sessionService.save(session);
        }
        if (dto != null && StringUtils.hasText(dto.getHealthAssistantSessionId())) {
            sendHealthAssistantHandoff(session, dto.getHealthAssistantSessionId());
        }
        return Result.success(toVO(session));
    }

    private void sendHealthAssistantHandoff(DoctorConsultSession session, String healthAssistantSessionId) {
        String summary = handoffService.summarize(session.getUserId(), healthAssistantSessionId);
        boolean alreadySent = messageService.count(Wrappers.<DoctorConsultMessage>lambdaQuery()
                .eq(DoctorConsultMessage::getSessionId, session.getId())
                .eq(DoctorConsultMessage::getSenderType, "USER")
                .eq(DoctorConsultMessage::getContent, summary)) > 0;
        if (alreadySent) return;

        DoctorConsultMessageDTO handoff = new DoctorConsultMessageDTO();
        handoff.setContent(summary);
        DoctorConsultMessage message = newMessage(session, "USER", handoff);
        messageService.save(message);
        session.setLastMessage(summary.substring(0, Math.min(summary.length(), 500)));
        session.setLastMessageTime(LocalDateTime.now());
        session.setUnreadDoctor((session.getUnreadDoctor() == null ? 0 : session.getUnreadDoctor()) + 1);
        sessionService.updateById(session);

        Doctor doctor = doctorService.getById(session.getDoctorId());
        if (doctor != null && doctor.getUserId() != null) {
            notifier.notifyUser(doctor.getUserId(), "DOCTOR_CONSULT_MESSAGE", message);
        }
    }

    @Operation(summary = "我的医生咨询会话")
    @GetMapping("/sessions")
    public Result<PageResult<DoctorConsultSessionVO>> sessions(@RequestParam(defaultValue = "1") long pageNum,
                                                               @RequestParam(defaultValue = "10") long pageSize) {
        var page = sessionService.page(new Page<>(pageNum, pageSize),
                Wrappers.<DoctorConsultSession>lambdaQuery()
                        .eq(DoctorConsultSession::getUserId, SecurityUtil.getUserId())
                        .orderByDesc(DoctorConsultSession::getLastMessageTime));
        return Result.success(toPageVO(page));
    }

    @Operation(summary = "会话消息")
    @GetMapping("/session/{sessionId}/messages")
    public Result<List<DoctorConsultMessage>> messages(@PathVariable Long sessionId) {
        DoctorConsultSession session = checkPatientSession(sessionId);
        session.setUnreadUser(0);
        sessionService.updateById(session);
        return Result.success(messageService.list(Wrappers.<DoctorConsultMessage>lambdaQuery()
                .eq(DoctorConsultMessage::getSessionId, sessionId)
                .orderByAsc(DoctorConsultMessage::getCreateTime)));
    }

    @Operation(summary = "患者发送消息")
    @PostMapping("/session/{sessionId}/messages")
    public Result<DoctorConsultMessage> send(@PathVariable Long sessionId,
                                             @Valid @RequestBody DoctorConsultMessageDTO dto) {
        DoctorConsultSession session = checkPatientSession(sessionId);
        ensureOpenSession(session);
        DoctorConsultMessage msg = newMessage(session, "USER", dto);
        messageService.save(msg);
        session.setLastMessage(summaryOf(msg));
        session.setLastMessageTime(LocalDateTime.now());
        session.setUnreadDoctor((session.getUnreadDoctor() == null ? 0 : session.getUnreadDoctor()) + 1);
        sessionService.updateById(session);

        Doctor doctor = doctorService.getById(session.getDoctorId());
        if (doctor != null && doctor.getUserId() != null) {
            notifier.notifyUser(doctor.getUserId(), "DOCTOR_CONSULT_MESSAGE", msg);
        }
        return Result.success("发送成功", msg);
    }

    @Operation(summary = "患者结束咨询会话")
    @PutMapping("/session/{sessionId}/close")
    public Result<DoctorConsultSessionVO> close(@PathVariable Long sessionId) {
        DoctorConsultSession session = checkPatientSession(sessionId);
        closeSessionIfOpen(session);
        Doctor doctor = doctorService.getById(session.getDoctorId());
        if (doctor != null && doctor.getUserId() != null) {
            notifier.notifyUser(doctor.getUserId(), "DOCTOR_CONSULT_SESSION_CLOSED", session);
        }
        return Result.success("会话已结束", toVO(session));
    }

    @Operation(summary = "患者提交咨询反馈")
    @PostMapping("/session/{sessionId}/feedback")
    public Result<DoctorConsultSessionVO> feedback(@PathVariable Long sessionId,
                                                   @Valid @RequestBody DoctorConsultFeedbackDTO dto) {
        DoctorConsultSession session = checkPatientSession(sessionId);
        if (!"CLOSED".equals(session.getStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "会话结束后才能评价");
        }
        if (!hasDoctorSummary(session)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "医生提交咨询报告后才能评价");
        }
        if (dto.getRating() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "请选择评分");
        }
        session.setRating(dto.getRating());
        session.setFeedbackTags(joinTags(dto.getTags()));
        session.setFeedback(cleanText(dto.getFeedback()));
        session.setFeedbackTime(LocalDateTime.now());
        sessionService.updateById(session);

        Doctor doctor = doctorService.getById(session.getDoctorId());
        if (doctor != null && doctor.getUserId() != null) {
            notifier.notifyUser(doctor.getUserId(), "DOCTOR_CONSULT_FEEDBACK", session);
        }
        return Result.success("评价已提交", toVO(session));
    }

    private DoctorConsultSession checkPatientSession(Long sessionId) {
        DoctorConsultSession session = sessionService.getById(sessionId);
        if (session == null || !session.getUserId().equals(SecurityUtil.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return session;
    }

    private void ensureOpenSession(DoctorConsultSession session) {
        if (!"OPEN".equals(session.getStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "会话已结束，不能继续发送消息");
        }
    }

    private void closeSessionIfOpen(DoctorConsultSession session) {
        if (!"OPEN".equals(session.getStatus())) {
            return;
        }
        session.setStatus("CLOSED");
        session.setLastMessage("[会话已结束]");
        session.setLastMessageTime(LocalDateTime.now());
        session.setUnreadDoctor(0);
        session.setUnreadUser(0);
        sessionService.updateById(session);
    }

    private String joinTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) return null;
        String value = String.join(",", tags.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .limit(8)
                .toList());
        return StringUtils.hasText(value) ? value : null;
    }

    private String cleanText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private boolean hasDoctorSummary(DoctorConsultSession session) {
        return StringUtils.hasText(session.getProblemOverview())
                || StringUtils.hasText(session.getPreliminaryAssessment())
                || StringUtils.hasText(session.getSummary())
                || StringUtils.hasText(session.getAdvice())
                || StringUtils.hasText(session.getRiskWarning());
    }

    private DoctorConsultMessage newMessage(DoctorConsultSession session, String senderType, DoctorConsultMessageDTO dto) {
        DoctorConsultMessage msg = new DoctorConsultMessage();
        msg.setSessionId(session.getId());
        msg.setUserId(session.getUserId());
        msg.setDoctorId(session.getDoctorId());
        msg.setSenderType(senderType);
        msg.setMessageType(dto.getAttachmentUrl() != null && !dto.getAttachmentUrl().isBlank() ? "ATTACHMENT" : "TEXT");
        msg.setContent(dto.getContent());
        msg.setAttachmentUrl(dto.getAttachmentUrl());
        msg.setAttachmentName(dto.getAttachmentName());
        msg.setReadFlag(0);
        return msg;
    }

    private String summaryOf(DoctorConsultMessage msg) {
        if ("ATTACHMENT".equals(msg.getMessageType())) {
            return "[附件] " + (msg.getAttachmentName() == null ? "文件" : msg.getAttachmentName());
        }
        return msg.getContent();
    }

    private DoctorConsultSessionVO toVO(DoctorConsultSession session) {
        DoctorConsultSessionVO vo = new DoctorConsultSessionVO();
        vo.setSession(session);
        vo.setDoctor(doctorService.getById(session.getDoctorId()));
        User patient = userService.getById(session.getUserId());
        if (patient != null) patient.setPassword(null);
        vo.setPatient(patient);
        return vo;
    }

    private PageResult<DoctorConsultSessionVO> toPageVO(com.baomidou.mybatisplus.core.metadata.IPage<DoctorConsultSession> page) {
        PageResult<DoctorConsultSessionVO> pr = new PageResult<>();
        pr.setTotal(page.getTotal());
        pr.setCurrent(page.getCurrent());
        pr.setSize(page.getSize());
        pr.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return pr;
    }
}
