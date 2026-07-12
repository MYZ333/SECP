package com.medcare.hda.aspect;

import com.medcare.hda.annotation.RateLimit;
import com.medcare.hda.common.ResultCode;
import com.medcare.hda.common.ratelimit.LimitDimension;
import com.medcare.hda.common.ratelimit.RedisRateLimiter;
import com.medcare.hda.exception.BusinessException;
import com.medcare.hda.security.LoginUser;
import com.medcare.hda.util.WebUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** 接口级限流切面：解析 @RateLimit，按维度组装 key，走 Redis+Lua 滑动窗口 */
@Slf4j
@Aspect
@Order(1)
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RedisRateLimiter rateLimiter;

    private static final String KEY_PREFIX = "hda:rl:";

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        String dimensionValue = resolveDimensionValue(rateLimit.dimension());
        String redisKey = KEY_PREFIX + rateLimit.key() + ":" + dimensionValue;
        long windowMillis = rateLimit.window() * 1000L;

        if (!rateLimiter.tryAcquire(redisKey, windowMillis, rateLimit.limit())) {
            log.warn("触发限流: key={}, 维度={}, 阈值={}/{}s",
                    redisKey, rateLimit.dimension(), rateLimit.limit(), rateLimit.window());
            String msg = rateLimit.message().isEmpty()
                    ? ResultCode.RATE_LIMIT_EXCEEDED.getMessage() : rateLimit.message();
            throw new BusinessException(ResultCode.RATE_LIMIT_EXCEEDED.getCode(), msg);
        }
        return pjp.proceed();
    }

    private String resolveDimensionValue(LimitDimension dimension) {
        return switch (dimension) {
            case USER -> {
                Long uid = currentUserId();
                yield uid != null ? "u" + uid : "anon";
            }
            case IP -> {
                HttpServletRequest req = WebUtil.currentRequest();
                yield WebUtil.getClientIp(req);
            }
            case GLOBAL -> "global";
        };
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser.getUserId();
        }
        return null;
    }
}
