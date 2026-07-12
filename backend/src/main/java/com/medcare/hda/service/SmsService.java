package com.medcare.hda.service;

/**
 * 短信验证码服务。
 * 阿里云短信正式接入；未配置 AccessKey 时自动降级为 mock 模式（验证码打印到后端日志）。
 */
public interface SmsService {

    /**
     * 发送登录/重置密码验证码。
     * 限流：同手机号 60 秒内不可重发、每天最多 10 条；同 IP 每小时最多 20 条。
     *
     * @return true 表示 mock 模式（验证码在后端日志中），false 表示真实短信已发出
     */
    boolean sendCode(String phone, String ip);

    /**
     * 校验验证码：5 分钟有效，连错 5 次作废。
     * 校验通过后验证码立即失效（一次性）。校验失败抛 BusinessException。
     */
    void verifyCode(String phone, String code);
}
