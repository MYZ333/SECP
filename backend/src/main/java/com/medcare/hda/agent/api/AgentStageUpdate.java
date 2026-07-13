package com.medcare.hda.agent.api;

/** 仅向用户暴露执行阶段，不暴露模型内部推理。 */
public record AgentStageUpdate(String stage, String status, String message) {
    public static AgentStageUpdate running(String stage, String message) {
        return new AgentStageUpdate(stage, "RUNNING", message);
    }

    public static AgentStageUpdate completed(String stage, String message) {
        return new AgentStageUpdate(stage, "COMPLETED", message);
    }

    public static AgentStageUpdate degraded(String stage, String message) {
        return new AgentStageUpdate(stage, "DEGRADED", message);
    }
}
