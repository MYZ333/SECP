package com.medcare.hda.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/** 统一分页返回结构 */
@Data
@Schema(description = "分页结果")
public class PageResult<T> implements Serializable {

    @Schema(description = "总记录数")
    private long total;

    @Schema(description = "当前页数据")
    private List<T> records;

    @Schema(description = "当前页码")
    private long current;

    @Schema(description = "每页大小")
    private long size;

    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> pr = new PageResult<>();
        pr.setTotal(page.getTotal());
        pr.setRecords(page.getRecords());
        pr.setCurrent(page.getCurrent());
        pr.setSize(page.getSize());
        return pr;
    }
}
