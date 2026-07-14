package com.medcare.hda.dto;

import com.medcare.hda.agent.memory.MemorySourceAgent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemorySearchRequest(
        @NotBlank @Size(max = 1000) String query,
        @Min(1) @Max(50) Integer topK,
        MemorySourceAgent consumer) {
}
