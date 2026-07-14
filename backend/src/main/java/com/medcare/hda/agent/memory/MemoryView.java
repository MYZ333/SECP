package com.medcare.hda.agent.memory;

import java.time.LocalDateTime;

public record MemoryView(String memoryId, String content, MemoryCategory category,
                         MemoryVisibility visibility, MemorySourceAgent sourceAgent,
                         double confidence, int version, LocalDateTime createTime,
                         LocalDateTime updateTime) {
}
