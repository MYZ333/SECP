package com.medcare.hda.controller;

import com.medcare.hda.agent.knowledge.KnowledgeAdminService;
import com.medcare.hda.agent.knowledge.KnowledgeChunkView;
import com.medcare.hda.agent.knowledge.KnowledgeDocumentView;
import com.medcare.hda.common.PageResult;
import com.medcare.hda.common.Result;
import com.medcare.hda.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "管理端-健康知识库")
@RestController
@RequestMapping("/api/admin/knowledge")
@RequiredArgsConstructor
public class AdminKnowledgeController {
    private final KnowledgeAdminService service;

    @GetMapping("/page")
    public Result<PageResult<KnowledgeDocumentView>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                          @RequestParam(defaultValue = "10") long pageSize,
                                                          @RequestParam(required = false) String keyword,
                                                          @RequestParam(required = false) String status) {
        return Result.success(service.page(pageNum, pageSize, keyword, status));
    }

    @GetMapping("/{id}/chunks")
    public Result<List<KnowledgeChunkView>> chunks(@PathVariable Long id) { return Result.success(service.chunks(id)); }

    @GetMapping("/application/page")
    public Result<PageResult<KnowledgeDocumentView>> applicationPage(@RequestParam(defaultValue = "1") long pageNum,
                                                                     @RequestParam(defaultValue = "10") long pageSize,
                                                                     @RequestParam(required = false) String keyword,
                                                                     @RequestParam(required = false) String status) {
        return Result.success(service.page(pageNum, pageSize, keyword, status, "APPLICATION"));
    }

    @Operation(summary = "上传并解析知识文档，初始状态为草稿")
    @PostMapping
    public Result<Long> upload(@RequestParam MultipartFile file, @RequestParam String title,
                               @RequestParam String sourceOrg, @RequestParam String sourceUrl,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate publishedDate,
                               @RequestParam(required = false) String versionNo, @RequestParam String category) {
        return Result.success("文档解析完成，请预览后发布",
                service.upload(file, title, sourceOrg, sourceUrl, publishedDate, versionNo, category));
    }

    @PostMapping("/application")
    public Result<Long> uploadApplication(@RequestParam MultipartFile file, @RequestParam String title,
                                          @RequestParam String sourceOrg, @RequestParam String sourceUrl,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate publishedDate,
                                          @RequestParam(required = false) String versionNo, @RequestParam String category) {
        return Result.success(service.upload(file, title, sourceOrg, sourceUrl, publishedDate, versionNo, category, "APPLICATION"));
    }

    @PostMapping("/application/seed")
    public Result<Integer> seedApplication() { return Result.success(service.importSeeds("APPLICATION")); }

    @PostMapping("/seed")
    public Result<Integer> seed() { return Result.success("首批语料导入完成", service.importSeeds()); }

    @PostMapping("/{id}/publish")
    public Result<Void> publish(@PathVariable Long id) {
        service.publish(id, SecurityUtil.getUserId()); return Result.success("发布成功", null);
    }

    @PostMapping("/{id}/reindex")
    public Result<Void> reindex(@PathVariable Long id) {
        service.reindex(id, SecurityUtil.getUserId()); return Result.success("索引重建成功", null);
    }

    @PutMapping("/{id}/inactive")
    public Result<Void> inactive(@PathVariable Long id) {
        service.inactive(id); return Result.success("文档已停用", null);
    }
}
