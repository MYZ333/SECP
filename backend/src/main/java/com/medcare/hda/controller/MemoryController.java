package com.medcare.hda.controller;

import com.medcare.hda.agent.memory.LongTermMemoryService;
import com.medcare.hda.agent.memory.MemoryCategory;
import com.medcare.hda.agent.memory.MemorySourceAgent;
import com.medcare.hda.agent.memory.MemoryView;
import com.medcare.hda.agent.memory.MemoryVisibility;
import com.medcare.hda.common.PageResult;
import com.medcare.hda.common.Result;
import com.medcare.hda.dto.MemoryCreateRequest;
import com.medcare.hda.dto.MemorySearchRequest;
import com.medcare.hda.dto.MemoryUpdateRequest;
import com.medcare.hda.security.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/memories")
@RequiredArgsConstructor
public class MemoryController {
    private final LongTermMemoryService memoryService;

    @GetMapping
    public Result<PageResult<MemoryView>> page(@RequestParam(defaultValue = "1") long pageNum,
                                               @RequestParam(defaultValue = "20") long pageSize,
                                               @RequestParam(required = false) MemoryCategory category,
                                               @RequestParam(required = false) MemoryVisibility visibility,
                                               @RequestParam(required = false) String keyword) {
        return Result.success(memoryService.page(SecurityUtil.getUserId(), pageNum, pageSize, category, visibility, keyword));
    }

    @PostMapping("/search")
    public Result<List<MemoryView>> search(@Valid @RequestBody MemorySearchRequest request) {
        MemorySourceAgent consumer = request.consumer() == null ? MemorySourceAgent.HEALTH : request.consumer();
        int topK = request.topK() == null ? 10 : request.topK();
        return Result.success(memoryService.search(SecurityUtil.getUserId(), request.query(), consumer, topK));
    }

    @PostMapping
    public Result<MemoryView> create(@Valid @RequestBody MemoryCreateRequest request) {
        return Result.success(memoryService.create(SecurityUtil.getUserId(), request));
    }

    @PutMapping("/{memoryId}")
    public Result<MemoryView> update(@PathVariable String memoryId, @Valid @RequestBody MemoryUpdateRequest request) {
        return Result.success(memoryService.update(SecurityUtil.getUserId(), memoryId, request));
    }

    @DeleteMapping("/{memoryId}")
    public Result<Void> delete(@PathVariable String memoryId) {
        memoryService.delete(SecurityUtil.getUserId(), memoryId);
        return Result.success("记忆已删除", null);
    }

    @DeleteMapping
    public Result<Void> clear() {
        memoryService.clear(SecurityUtil.getUserId());
        return Result.success("长期记忆已清空", null);
    }
}
