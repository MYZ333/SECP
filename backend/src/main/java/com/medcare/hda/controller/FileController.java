package com.medcare.hda.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.medcare.hda.common.Result;
import com.medcare.hda.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Set;

/** 文件上传（头像等图片） */
@Tag(name = "文件上传")
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    @Value("${hda.upload.dir:./uploads}")
    private String uploadDir;

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final long MAX_SIZE = 2 * 1024 * 1024; // 2MB
    private static final Set<String> ATTACHMENT_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp", "pdf", "doc", "docx");
    private static final long ATTACHMENT_MAX_SIZE = 10 * 1024 * 1024; // 10MB

    @Operation(summary = "上传头像图片, 返回可访问的URL")
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的图片");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException("图片不能超过 2MB");
        }
        String ext = FileUtil.extName(file.getOriginalFilename());
        if (ext == null || !ALLOWED_EXT.contains(ext.toLowerCase())) {
            throw new BusinessException("仅支持 jpg/png/gif/webp 格式图片");
        }
        try {
            File dir = new File(uploadDir, "avatar");
            if (!dir.exists() && !dir.mkdirs()) {
                throw new BusinessException("上传目录创建失败");
            }
            String filename = IdUtil.simpleUUID() + "." + ext.toLowerCase();
            file.transferTo(new File(dir, filename).getAbsoluteFile());
            return Result.success("上传成功", "/uploads/avatar/" + filename);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("上传失败: " + e.getMessage());
        }
    }

    @Operation(summary = "上传医生咨询附件, 返回可访问的URL")
    @PostMapping("/consult-attachment")
    public Result<String> uploadConsultAttachment(@RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }
        if (file.getSize() > ATTACHMENT_MAX_SIZE) {
            throw new BusinessException("附件不能超过 10MB");
        }
        String ext = FileUtil.extName(file.getOriginalFilename());
        if (ext == null || !ATTACHMENT_EXT.contains(ext.toLowerCase())) {
            throw new BusinessException("仅支持图片、PDF、Word 附件");
        }
        try {
            File dir = new File(uploadDir, "consult");
            if (!dir.exists() && !dir.mkdirs()) {
                throw new BusinessException("上传目录创建失败");
            }
            String filename = IdUtil.simpleUUID() + "." + ext.toLowerCase();
            file.transferTo(new File(dir, filename).getAbsoluteFile());
            return Result.success("上传成功", "/uploads/consult/" + filename);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("上传失败: " + e.getMessage());
        }
    }
}
