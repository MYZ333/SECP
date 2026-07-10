package com.medcare.hda.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 分页查询基础参数 */
@Data
@Schema(description = "分页查询参数")
public class PageQuery {
    @Schema(description = "页码, 从 1 开始", example = "1")
    private long pageNum = 1;

    @Schema(description = "每页条数", example = "10")
    private long pageSize = 10;

    @Schema(description = "关键词（可选）")
    private String keyword;
}
