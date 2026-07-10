package com.medcare.hda.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medcare.hda.common.PageResult;
import com.medcare.hda.common.Result;
import com.medcare.hda.dto.ChatDTO;
import com.medcare.hda.entity.ConsultRecord;
import com.medcare.hda.security.SecurityUtil;
import com.medcare.hda.service.AiChatService;
import com.medcare.hda.service.ConsultRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "健康咨询", description = "AI模块-健康咨询对话")
@RestController
@RequestMapping("/api/consult")
@RequiredArgsConstructor
public class ConsultController {

    private final AiChatService aiChatService;
    private final ConsultRecordService consultRecordService;

    @Operation(summary = "发起健康咨询")
    @PostMapping("/chat")
    public Result<ConsultRecord> chat(@Valid @RequestBody ChatDTO dto) {
        Long userId = SecurityUtil.getUserId();
        String sessionId = StringUtils.hasText(dto.getSessionId())
                ? dto.getSessionId() : UUID.randomUUID().toString();

        // 保存用户消息
        ConsultRecord userMsg = new ConsultRecord();
        userMsg.setUserId(userId);
        userMsg.setSessionId(sessionId);
        userMsg.setRole("user");
        userMsg.setContent(dto.getMessage());
        consultRecordService.save(userMsg);

        // 调用 AI（骨架, 占位）
        String answer = aiChatService.consult(userId, sessionId, dto.getMessage());

        // 保存 AI 回复
        ConsultRecord aiMsg = new ConsultRecord();
        aiMsg.setUserId(userId);
        aiMsg.setSessionId(sessionId);
        aiMsg.setRole("assistant");
        aiMsg.setContent(answer);
        consultRecordService.save(aiMsg);

        return Result.success(aiMsg);
    }

    @Operation(summary = "咨询历史(分页)")
    @GetMapping("/history")
    public Result<PageResult<ConsultRecord>> history(@RequestParam(required = false) String sessionId,
                                                     @RequestParam(defaultValue = "1") long pageNum,
                                                     @RequestParam(defaultValue = "20") long pageSize) {
        Long userId = SecurityUtil.getUserId();
        var page = consultRecordService.page(new Page<>(pageNum, pageSize),
                Wrappers.<ConsultRecord>lambdaQuery()
                        .eq(ConsultRecord::getUserId, userId)
                        .eq(StringUtils.hasText(sessionId), ConsultRecord::getSessionId, sessionId)
                        .orderByAsc(ConsultRecord::getCreateTime));
        return Result.success(PageResult.of(page));
    }
}
