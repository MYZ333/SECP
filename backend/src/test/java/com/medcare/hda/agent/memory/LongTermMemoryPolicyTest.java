package com.medcare.hda.agent.memory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LongTermMemoryPolicyTest {

    @Test
    void healthFactsMustAlwaysRemainPrivate() {
        assertEquals(MemoryVisibility.HEALTH_PRIVATE,
                LongTermMemoryService.normalizeVisibility(MemoryCategory.HEALTH_FACT, MemoryVisibility.SHARED));
    }

    @Test
    void nonHealthMemoryKeepsRequestedVisibility() {
        assertEquals(MemoryVisibility.SHARED,
                LongTermMemoryService.normalizeVisibility(MemoryCategory.PREFERENCE, MemoryVisibility.SHARED));
    }
}
