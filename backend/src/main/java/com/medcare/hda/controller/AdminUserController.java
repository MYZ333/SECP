package com.medcare.hda.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medcare.hda.common.PageResult;
import com.medcare.hda.common.Result;
import com.medcare.hda.entity.User;
import com.medcare.hda.service.PointService;
import com.medcare.hda.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理端-用户管理")
@RestController
@RequestMapping("/api/admin/user")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;
    private final PointService pointService;

    @Operation(summary = "用户列表(分页)")
    @GetMapping("/page")
    public Result<PageResult<User>> page(@RequestParam(defaultValue = "1") long pageNum,
                                         @RequestParam(defaultValue = "10") long pageSize,
                                         @RequestParam(required = false) String keyword) {
        IPage<User> page = userService.page(new Page<>(pageNum, pageSize),
                Wrappers.<User>lambdaQuery()
                        .and(StringUtils.hasText(keyword), w -> w
                                .like(User::getUsername, keyword)
                                .or()
                                .like(User::getNickname, keyword))
                        .orderByDesc(User::getCreateTime));
        page.getRecords().forEach(u -> {
            userService.populateUserSnapshot(u);
            u.setPassword(null);
        });
        return Result.success(PageResult.of(page));
    }

    @Operation(summary = "启用/禁用用户")
    @PutMapping("/{id}/status/{status}")
    public Result<Void> changeStatus(@PathVariable Long id, @PathVariable Integer status) {
        User user = userService.getById(id);
        if (user != null) {
            user.setStatus(status);
            userService.updateById(user);
        }
        return Result.success("操作成功", null);
    }

    @Operation(summary = "管理员调整用户积分")
    @PostMapping("/{id}/points")
    public Result<Void> adjustPoints(@PathVariable Long id, @RequestParam Integer change,
                                     @RequestParam(required = false) String remark) {
        pointService.addPoints(id, change, "ADMIN", remark != null ? remark : "管理员调整");
        return Result.success("操作成功", null);
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.removeById(id);
        return Result.success("删除成功", null);
    }
}
