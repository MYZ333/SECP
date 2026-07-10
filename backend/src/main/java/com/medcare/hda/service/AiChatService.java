package com.medcare.hda.service;

/**
 * Spring AI 服务骨架（登录/档案组只搭结构，具体模型接入由 AI 组负责）。
 * 约定：所有与大模型交互的能力统一收口到本接口，方便后续替换实现。
 */
public interface AiChatService {

    /** 健康咨询：根据用户提问返回回答（当前为占位实现） */
    String consult(Long userId, String sessionId, String message);

    /** 健康预警：根据用户近期体征数据生成预警建议（当前为占位实现） */
    String analyzeHealthRisk(Long userId);
}
