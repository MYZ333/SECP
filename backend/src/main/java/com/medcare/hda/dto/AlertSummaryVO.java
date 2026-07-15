package com.medcare.hda.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/** 健康预警列表顶部统计。 */
@Data
@AllArgsConstructor
public class AlertSummaryVO {
    private long total;
    private long active;
    private long unread;
    private long highUnread;
    private long mediumUnread;
    private long open;
    private long inProgress;
    private long highActive;
    private long resolved;
    private LocalDateTime latestCreateTime;
}
