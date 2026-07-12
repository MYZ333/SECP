package com.medcare.hda.controller;

import cn.hutool.core.util.IdUtil;
import com.medcare.hda.aspect.IdempotentAspect;
import com.medcare.hda.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/** 幂等令牌：提交类接口前先领取，随请求头 Idempotency-Key 带回 */
@Tag(name = "幂等令牌", description = "提交前领取一次性令牌，防重复提交")
@RestController
@RequestMapping("/api/idempotent")
@RequiredArgsConstructor
public class IdempotentController {

    private final StringRedisTemplate redisTemplate;

    /** 令牌有效期：5 分钟内未使用则过期 */
    private static final long TTL_MINUTES = 5;

    @Operation(summary = "领取幂等令牌")
    @GetMapping("/token")
    public Result<Map<String, String>> token() {
        String token = IdUtil.fastSimpleUUID();
        redisTemplate.opsForValue().set(
                IdempotentAspect.TOKEN_PREFIX + token, "1", TTL_MINUTES, TimeUnit.MINUTES);
        return Result.success(Map.of("idempotencyKey", token));
    }
}
