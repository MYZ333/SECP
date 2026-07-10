package com.medcare.hda.service.impl;

import com.medcare.hda.service.AiChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * AI 服务骨架实现。
 * 已注入 Spring AI 的 ChatClient.Builder（若配置了可用的模型 api-key 即可直接调用）。
 * 目前为“可运行占位”：未配置真实模型时返回占位文案，配置后放开注释即可对接。
 *
 * TODO(AI 组):
 *   1. 在 application.yml 配置 spring.ai.openai.* 或替换为通义/智谱等 starter；
 *   2. 打开 chatClient().prompt()... 调用；
 *   3. 接入 RAG（医生专家库/健康知识库）与 system prompt。
 */
@Slf4j
@Service
public class AiChatServiceImpl implements AiChatService {

    private final ObjectProvider<ChatClient.Builder> chatClientBuilder;

    public AiChatServiceImpl(ObjectProvider<ChatClient.Builder> chatClientBuilder) {
        this.chatClientBuilder = chatClientBuilder;
    }

    private static final String SYSTEM_PROMPT = """
            你是"智慧医养"平台的健康助手，面向老年人及其家属，提供健康咨询建议。
            请用通俗、亲切的语言回答，涉及严重症状时提醒及时就医，不做确诊。
            """;

    @Override
    public String consult(Long userId, String sessionId, String message) {
        ChatClient.Builder builder = chatClientBuilder.getIfAvailable();
        if (builder == null) {
            return placeholder(message);
        }
        try {
            return builder.build()
                    .prompt()
                    .system(SYSTEM_PROMPT)
                    .user(message)
                    .call()
                    .content();
        } catch (Exception e) {
            log.warn("AI 调用失败(可能未配置有效模型), 返回占位内容: {}", e.getMessage());
            return placeholder(message);
        }
    }

    @Override
    public String analyzeHealthRisk(Long userId) {
        // TODO(AI 组): 拉取该用户近期 health_metric 数据，构造 prompt 交给模型分析
        return "【健康预警-占位】暂未接入AI模型。接入后将根据您近期的血压、血糖、心率等数据自动生成风险提示与建议。";
    }

    private String placeholder(String message) {
        return "【健康咨询-占位回复】您咨询的是：\"" + message
                + "\"。AI 模型尚未接入，接入后此处将返回智能回答。";
    }
}
