package com.medcare.hda.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medcare.hda.annotation.Idempotent;
import com.medcare.hda.common.PageResult;
import com.medcare.hda.common.Result;
import com.medcare.hda.dto.CheckInVO;
import com.medcare.hda.dto.ExchangeDTO;
import com.medcare.hda.dto.PointTaskVO;
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

    @Operation(summary = "每日签到(连续签到有加成)")
    @PostMapping("/check-in")
    public Result<CheckInVO> checkIn() {
        CheckInVO vo = pointService.checkIn(SecurityUtil.getUserId());
        return Result.success("签到成功, 积分+" + vo.getPoints(), vo);
    }

    @Operation(summary = "积分任务列表(各获取方式及完成状态)")
    @GetMapping("/tasks")
    public Result<java.util.List<PointTaskVO>> tasks() {
        return Result.success(pointService.listTasks(SecurityUtil.getUserId()));
    }

    @Operation(summary = "领取任务积分(待领取 → 已完成)")
    @PostMapping("/claim/{type}")
    public Result<com.medcare.hda.dto.ClaimVO> claim(@PathVariable String type) {
        com.medcare.hda.dto.ClaimVO vo = pointService.claimTask(SecurityUtil.getUserId(), type);
        return Result.success("领取成功，积分 +" + vo.getPoints(), vo);
    }

    @Operation(summary = "积分明细(分页)")
    @GetMapping("/records")
    public Result<PageResult<PointRecord>> records(@RequestParam(defaultValue = "1") long pageNum,
                                                   @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(PageResult.of(
                pointService.pageRecords(SecurityUtil.getUserId(), pageNum, pageSize)));
    }

    @Operation(summary = "积分商城-上架商品(分页, 可按类别筛选)")
    @GetMapping("/products")
    public Result<PageResult<PointProduct>> products(@RequestParam(defaultValue = "1") long pageNum,
                                                     @RequestParam(defaultValue = "10") long pageSize,
                                                     @RequestParam(required = false) String category) {
        IPage<PointProduct> page = productService.page(new Page<>(pageNum, pageSize),
                Wrappers.<PointProduct>lambdaQuery()
                        .eq(PointProduct::getStatus, 1)
                        .eq(org.springframework.util.StringUtils.hasText(category), PointProduct::getCategory, category)
                        .orderByAsc(PointProduct::getPointsCost));
        return Result.success(PageResult.of(page));
    }

    @Operation(summary = "积分商城-商品类别列表")
    @GetMapping("/categories")
    public Result<java.util.List<String>> categories() {
        return Result.success(productService.listActiveCategories());
    }

    @Operation(summary = "兑换商品(需先领取幂等令牌，随 Idempotency-Key 头提交)")
    @Idempotent(message = "兑换请求已提交，请勿重复点击")
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
