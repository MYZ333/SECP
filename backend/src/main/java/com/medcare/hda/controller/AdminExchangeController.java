package com.medcare.hda.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medcare.hda.common.PageResult;
import com.medcare.hda.common.Result;
import com.medcare.hda.entity.PointExchange;
import com.medcare.hda.service.PointExchangeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理端-兑换订单管理")
@RestController
@RequestMapping("/api/admin/exchange")
@RequiredArgsConstructor
public class AdminExchangeController {

    private final PointExchangeService service;

    @Operation(summary = "兑换订单列表(分页)")
    @GetMapping("/page")
    public Result<PageResult<PointExchange>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                  @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(PageResult.of(service.page(new Page<>(pageNum, pageSize),
                Wrappers.<PointExchange>lambdaQuery().orderByDesc(PointExchange::getCreateTime))));
    }

    @Operation(summary = "更新订单状态(0待发货1已发货2完成3取消)")
    @PutMapping("/{id}/status/{status}")
    public Result<Void> changeStatus(@PathVariable Long id, @PathVariable Integer status) {
        PointExchange e = service.getById(id);
        if (e != null) {
            e.setStatus(status);
            service.updateById(e);
        }
        return Result.success("操作成功", null);
    }
}
