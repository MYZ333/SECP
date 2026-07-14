package com.medcare.hda.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medcare.hda.common.PageResult;
import com.medcare.hda.common.Result;
import com.medcare.hda.common.ResultCode;
import com.medcare.hda.dto.DoctorConsultMessageDTO;
import com.medcare.hda.dto.DoctorConsultSessionVO;
import com.medcare.hda.dto.DoctorStatsVO;
import com.medcare.hda.dto.PatientDetailVO;
import com.medcare.hda.entity.*;
import com.medcare.hda.exception.BusinessException;
import com.medcare.hda.mapper.UserMapper;
import com.medcare.hda.security.SecurityUtil;
import com.medcare.hda.service.*;
import com.medcare.hda.websocket.DoctorConsultNotifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;

@Tag(name = "医生端", description = "医生工作台 / 患者 / 咨询")
@RestController
@RequestMapping("/api/doctor-portal")
@RequiredArgsConstructor
public class DoctorPortalController {

    private final DoctorService doctorService;
    private final UserService userService;
    private final DoctorConsultSessionService sessionService;
    private final DoctorConsultMessageService messageService;
    private final HealthProfileService profileService;
    private final HealthMetricService metricService;
    private final MedicalRecordService medicalRecordService;
    private final HealthReportService reportService;
    private final HealthAlertService alertService;
    private final DoctorConsultNotifier notifier;
    private final UserMapper userMapper;

    @Operation(summary = "当前医生资料")
    @GetMapping("/me")
    public Result<Doctor> me() {
        return Result.success(currentDoctor());
    }

    @Operation(summary = "更新当前医生资料")
    @PutMapping("/me")
    public Result<Doctor> updateMe(@RequestBody Doctor payload) {
        Doctor doctor = currentDoctor();
        doctor.setName(payload.getName());
        doctor.setPhone(payload.getPhone());
        doctor.setAvatar(payload.getAvatar());
        doctor.setHospital(payload.getHospital());
        doctor.setDepartment(payload.getDepartment());
        doctor.setTitle(payload.getTitle());
        doctor.setSpeciality(payload.getSpeciality());
        doctor.setIntroduction(payload.getIntroduction());
        doctorService.updateById(doctor);
        return Result.success("保存成功", doctor);
    }

    @Operation(summary = "医生工作台统计")
    @GetMapping("/stats")
    public Result<DoctorStatsVO> stats() {
        Doctor doctor = currentDoctor();
        List<DoctorConsultSession> sessions = sessionService.list(Wrappers.<DoctorConsultSession>lambdaQuery()
                .eq(DoctorConsultSession::getDoctorId, doctor.getId()));
        long patientCount = userMapper.countActivePatients();
        long unread = sessions.stream().mapToLong(s -> s.getUnreadDoctor() == null ? 0 : s.getUnreadDoctor()).sum();
        long todayMessages = messageService.count(Wrappers.<DoctorConsultMessage>lambdaQuery()
                .eq(DoctorConsultMessage::getDoctorId, doctor.getId())
                .ge(DoctorConsultMessage::getCreateTime, LocalDate.now().atStartOfDay()));
        return Result.success(DoctorStatsVO.builder()
                .patientCount(patientCount)
                .openSessionCount(sessions.stream().filter(s -> "OPEN".equals(s.getStatus())).count())
                .unreadCount(unread)
                .todayMessageCount(todayMessages)
                .build());
    }

    @Operation(summary = "患者列表")
    @GetMapping("/patients")
    public Result<PageResult<User>> patients(@RequestParam(defaultValue = "1") long pageNum,
                                             @RequestParam(defaultValue = "10") long pageSize,
                                             @RequestParam(required = false) String keyword) {
        currentDoctor();
        var page = userMapper.pageActivePatients(new Page<>(pageNum, pageSize), keyword);
        page.getRecords().forEach(u -> {
            userService.populateUserSnapshot(u);
            u.setPassword(null);
        });
        return Result.success(PageResult.of(page));
    }

    @Operation(summary = "患者详情")
    @GetMapping("/patient/{userId}")
    public Result<PatientDetailVO> patientDetail(@PathVariable Long userId) {
        currentDoctor();
        User patient = userService.getById(userId);
        if (patient == null || !userService.hasRole(userId, "PATIENT") || patient.getStatus() == null || patient.getStatus() != 0) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "患者不存在");
        }
        patient.setPassword(null);
        PatientDetailVO vo = new PatientDetailVO();
        vo.setPatient(patient);
        vo.setAge(patient != null && patient.getBirthday() != null
                ? Period.between(patient.getBirthday(), LocalDate.now()).getYears() : null);
        vo.setProfile(profileService.getOne(Wrappers.<HealthProfile>lambdaQuery()
                .eq(HealthProfile::getUserId, userId).last("LIMIT 1")));
        vo.setMetrics(metricService.list(Wrappers.<HealthMetric>lambdaQuery()
                .eq(HealthMetric::getUserId, userId).orderByDesc(HealthMetric::getMeasureTime).last("LIMIT 10")));
        vo.setMedicalRecords(medicalRecordService.list(Wrappers.<MedicalRecord>lambdaQuery()
                .eq(MedicalRecord::getUserId, userId).orderByDesc(MedicalRecord::getVisitDate).last("LIMIT 10")));
        vo.setReports(reportService.list(Wrappers.<HealthReport>lambdaQuery()
                .eq(HealthReport::getUserId, userId).orderByDesc(HealthReport::getReportDate).last("LIMIT 10")));
        vo.setAlerts(alertService.list(Wrappers.<HealthAlert>lambdaQuery()
                .eq(HealthAlert::getUserId, userId).orderByDesc(HealthAlert::getCreateTime).last("LIMIT 10")));
        return Result.success(vo);
    }

    @Operation(summary = "医生会话列表")
    @GetMapping("/sessions")
    public Result<PageResult<DoctorConsultSessionVO>> sessions(@RequestParam(defaultValue = "1") long pageNum,
                                                               @RequestParam(defaultValue = "10") long pageSize) {
        Doctor doctor = currentDoctor();
        var page = sessionService.page(new Page<>(pageNum, pageSize),
                Wrappers.<DoctorConsultSession>lambdaQuery()
                        .eq(DoctorConsultSession::getDoctorId, doctor.getId())
                        .orderByDesc(DoctorConsultSession::getLastMessageTime));
        return Result.success(toPageVO(page));
    }

    @Operation(summary = "医生查看消息")
    @GetMapping("/session/{sessionId}/messages")
    public Result<List<DoctorConsultMessage>> messages(@PathVariable Long sessionId) {
        DoctorConsultSession session = checkDoctorSession(sessionId);
        session.setUnreadDoctor(0);
        sessionService.updateById(session);
        return Result.success(messageService.list(Wrappers.<DoctorConsultMessage>lambdaQuery()
                .eq(DoctorConsultMessage::getSessionId, sessionId)
                .orderByAsc(DoctorConsultMessage::getCreateTime)));
    }

    @Operation(summary = "医生发送消息")
    @PostMapping("/session/{sessionId}/messages")
    public Result<DoctorConsultMessage> send(@PathVariable Long sessionId,
                                             @Valid @RequestBody DoctorConsultMessageDTO dto) {
        DoctorConsultSession session = checkDoctorSession(sessionId);
        ensureOpenSession(session);
        DoctorConsultMessage msg = new DoctorConsultMessage();
        msg.setSessionId(session.getId());
        msg.setUserId(session.getUserId());
        msg.setDoctorId(session.getDoctorId());
        msg.setSenderType("DOCTOR");
        msg.setMessageType(dto.getAttachmentUrl() != null && !dto.getAttachmentUrl().isBlank() ? "ATTACHMENT" : "TEXT");
        msg.setContent(dto.getContent());
        msg.setAttachmentUrl(dto.getAttachmentUrl());
        msg.setAttachmentName(dto.getAttachmentName());
        msg.setReadFlag(0);
        messageService.save(msg);

        session.setLastMessage("ATTACHMENT".equals(msg.getMessageType())
                ? "[附件] " + (msg.getAttachmentName() == null ? "文件" : msg.getAttachmentName())
                : msg.getContent());
        session.setLastMessageTime(LocalDateTime.now());
        session.setUnreadUser((session.getUnreadUser() == null ? 0 : session.getUnreadUser()) + 1);
        sessionService.updateById(session);
        notifier.notifyUser(session.getUserId(), "DOCTOR_CONSULT_MESSAGE", msg);
        return Result.success("发送成功", msg);
    }

    @Operation(summary = "医生结束咨询会话")
    @PutMapping("/session/{sessionId}/close")
    public Result<DoctorConsultSessionVO> closeSession(@PathVariable Long sessionId) {
        DoctorConsultSession session = checkDoctorSession(sessionId);
        closeSessionIfOpen(session);
        notifier.notifyUser(session.getUserId(), "DOCTOR_CONSULT_SESSION_CLOSED", session);
        return Result.success("会话已结束", toVO(session));
    }

    private Doctor currentDoctor() {
        if (!SecurityUtil.hasRole("DOCTOR")) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        Doctor doctor = doctorService.getOne(Wrappers.<Doctor>lambdaQuery()
                .eq(Doctor::getUserId, SecurityUtil.getUserId())
                .last("LIMIT 1"));
        if (doctor == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "医生资料不存在");
        }
        return doctor;
    }

    private DoctorConsultSession checkDoctorSession(Long sessionId) {
        Doctor doctor = currentDoctor();
        DoctorConsultSession session = sessionService.getById(sessionId);
        if (session == null || !session.getDoctorId().equals(doctor.getId())) {
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
