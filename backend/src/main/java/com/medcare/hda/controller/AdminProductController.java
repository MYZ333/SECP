package com.medcare.hda.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medcare.hda.common.PageResult;
import com.medcare.hda.common.Result;
import com.medcare.hda.entity.PointProduct;
import com.medcare.hda.service.PointProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理端-积分商品维护")
@RestController
@RequestMapping("/api/admin/product")
@RequiredArgsConstructor
public class AdminProductController {

    private final PointProductService service;

    @Operation(summary = "商品列表(分页)")
    @GetMapping("/page")
    public Result<PageResult<PointProduct>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                 @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(PageResult.of(service.page(new Page<>(pageNum, pageSize))));
    }

    @Operation(summary = "新增商品")
    @PostMapping
    public Result<PointProduct> create(@RequestBody PointProduct product) {
        product.setId(null);
        service.save(product);
        return Result.success("新增成功", product);
    }

    @Operation(summary = "修改商品")
    @PutMapping
    public Result<PointProduct> update(@RequestBody PointProduct product) {
        service.updateById(product);
        return Result.success("修改成功", product);
    }

    @Operation(summary = "删除商品")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.removeById(id);
        return Result.success("删除成功", null);
    }
}
