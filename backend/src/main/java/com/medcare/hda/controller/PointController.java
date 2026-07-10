package com.medcare.hda.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medcare.hda.common.PageResult;
import com.medcare.hda.common.Result;
import com.medcare.hda.dto.ExchangeDTO;
import com.medcare.hda.entity.PointExchange;
import com.medcare.hda.entity.PointProduct;
import com.medcare.hda.entity.PointRecord;
import com.medcare.hda.entity.User;
import com.medcare.hda.security.SecurityUtil;
import com.medcare.hda.service.PointProductService;
import com.medcare.hda.service.PointService;
import com.medcare.hda.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "个人积分", description = "积分余额 / 明细 / 商城 / 兑换")
@RestController
@RequestMapping("/api/point")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;
    private final PointProductService productService;
    private final UserService userService;

    @Operation(summary = "我的积分余额")
    @GetMapping("/balance")
    public Result<Integer> balance() {
        User user = userService.getById(SecurityUtil.getUserId());
        return Result.success(user == null || user.getPoints() == null ? 0 : user.getPoints());
    }

    @Operation(summary = "积分明细(分页)")
    @GetMapping("/records")
    public Result<PageResult<PointRecord>> records(@RequestParam(defaultValue = "1") long pageNum,
                                                   @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(PageResult.of(
                pointService.pageRecords(SecurityUtil.getUserId(), pageNum, pageSize)));
    }

    @Operation(summary = "积分商城-上架商品(分页)")
    @GetMapping("/products")
    public Result<PageResult<PointProduct>> products(@RequestParam(defaultValue = "1") long pageNum,
                                                     @RequestParam(defaultValue = "10") long pageSize) {
        IPage<PointProduct> page = productService.page(new Page<>(pageNum, pageSize),
                Wrappers.<PointProduct>lambdaQuery()
                        .eq(PointProduct::getStatus, 1)
                        .orderByAsc(PointProduct::getPointsCost));
        return Result.success(PageResult.of(page));
    }

    @Operation(summary = "兑换商品")
    @PostMapping("/exchange")
    public Result<PointExchange> exchange(@Valid @RequestBody ExchangeDTO dto) {
        return Result.success("兑换成功", pointService.exchange(SecurityUtil.getUserId(), dto));
    }

    @Operation(summary = "我的兑换记录(分页)")
    @GetMapping("/exchanges")
    public Result<PageResult<PointExchange>> exchanges(@RequestParam(defaultValue = "1") long pageNum,
                                                       @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(PageResult.of(
                pointService.pageExchanges(SecurityUtil.getUserId(), pageNum, pageSize)));
    }
}
