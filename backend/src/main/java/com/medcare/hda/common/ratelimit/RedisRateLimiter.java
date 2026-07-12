package com.medcare.hda.common.ratelimit;

import cn.hutool.core.util.IdUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * 基于 Redis + Lua 的滑动窗口限流器。
 * Lua 脚本保证"判断-计数-过期"三步原子执行，避免高并发下漏限。
 */
@Component
@RequiredArgsConstructor
public class RedisRateLimiter {

    private final StringRedisTemplate redisTemplate;
    private DefaultRedisScript<Long> script;

    @PostConstruct
    public void init() {
        script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("lua/sliding_window_rate_limit.lua")));
        script.setResultType(Long.class);
    }

    /**
     * @param key         限流 key（调用方已拼好维度）
     * @param windowMillis 窗口毫秒
     * @param limit       窗口内允许的最大请求数
     * @return true 放行，false 超限
     */
    public boolean tryAcquire(String key, long windowMillis, int limit) {
        long now = System.currentTimeMillis();
        String member = now + "-" + IdUtil.fastSimpleUUID();
        Long allowed = redisTemplate.execute(
                script,
                Collections.singletonList(key),
                String.valueOf(windowMillis),
                String.valueOf(limit),
                String.valueOf(now),
                member);
        return allowed != null && allowed == 1L;
    }
}
