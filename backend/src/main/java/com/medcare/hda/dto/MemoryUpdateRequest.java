package com.medcare.hda.dto;

import com.medcare.hda.agent.memory.MemoryCategory;
import com.medcare.hda.agent.memory.MemoryVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MemoryUpdateRequest(
        @NotBlank @Size(max = 1000) String content,
        @NotNull MemoryCategory category,
        @NotNull MemoryVisibility visibility) {
}
