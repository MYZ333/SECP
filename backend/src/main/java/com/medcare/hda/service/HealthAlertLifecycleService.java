package com.medcare.hda.service;

import com.medcare.hda.common.ResultCode;
import com.medcare.hda.dto.AlertActionDTO;
import com.medcare.hda.entity.HealthAlert;
import com.medcare.hda.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Set;

/** 健康预警查看、跟进、解决和忽略的状态流转。 */
@Service
@RequiredArgsConstructor
public class HealthAlertLifecycleService {
    private static final Set<String> ACTIVE = Set.of("OPEN", "ACKNOWLEDGED", "IN_PROGRESS");
    private static final Set<String> CHANNELS = Set.of("HEALTH_ASSISTANT", "DOCTOR_CONSULT");

    private final HealthAlertService alertService;

    @Transactional
    public HealthAlert acknowledge(Long id, Long userId) {
        HealthAlert alert = owned(id, userId);
        alert.setReadFlag(1);
        if ("OPEN".equals(statusOf(alert))) alert.setStatus("ACKNOWLEDGED");
        alertService.updateById(alert);
        return alert;
    }

    @Transactional
    public HealthAlert startHandling(Long id, Long userId, AlertActionDTO action) {
        HealthAlert alert = owned(id, userId);
        if (!ACTIVE.contains(statusOf(alert))) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "该预警已经结束，不能再次进入处理");
        }
        String channel = action == null ? null : action.getChannel();
        if (!CHANNELS.contains(channel)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "请选择有效的咨询渠道");
        }
        alert.setStatus("IN_PROGRESS");
        alert.setReadFlag(1);
        alert.setHandlingChannel(channel);
        alert.setRelatedSessionId(clean(action.getSessionId()));
        alertService.updateById(alert);
        return alert;
    }

    @Transactional
    public HealthAlert resolve(Long id, Long userId, AlertActionDTO action) {
        return finish(id, userId, "RESOLVED", action == null ? null : action.getNote());
    }

    @Transactional
    public HealthAlert ignore(Long id, Long userId, AlertActionDTO action) {
        String note = action == null ? null : clean(action.getNote());
        if (note == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "请填写忽略原因");
        }
        return finish(id, userId, "IGNORED", note);
    }

    private HealthAlert finish(Long id, Long userId, String target, String note) {
        HealthAlert alert = owned(id, userId);
        if (!ACTIVE.contains(statusOf(alert))) return alert;
        alert.setStatus(target);
        alert.setReadFlag(1);
        alert.setResolutionNote(clean(note));
        alert.setResolvedTime(LocalDateTime.now());
        alertService.updateById(alert);
        return alert;
    }

    private HealthAlert owned(Long id, Long userId) {
        HealthAlert alert = alertService.getById(id);
        if (alert == null) throw new BusinessException(ResultCode.NOT_FOUND);
        if (!userId.equals(alert.getUserId())) throw new BusinessException(ResultCode.FORBIDDEN);
        return alert;
    }

    private String statusOf(HealthAlert alert) {
        return StringUtils.hasText(alert.getStatus()) ? alert.getStatus() : (alert.getReadFlag() != null && alert.getReadFlag() == 1 ? "ACKNOWLEDGED" : "OPEN");
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
