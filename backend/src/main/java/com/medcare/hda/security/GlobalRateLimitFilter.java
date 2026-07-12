package com.medcare.hda.security;

import com.medcare.hda.common.ResultCode;
import com.medcare.hda.common.ratelimit.RedisRateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 全局 QPS 兜底限流：系统级雪崩保护，先于业务与鉴权执行。
 * 仅拦 /api/** 业务接口，放行文档/静态资源。窗口 1 秒，阈值可配。
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@Component
@RequiredArgsConstructor
public class GlobalRateLimitFilter extends OncePerRequestFilter {

    private final RedisRateLimiter rateLimiter;

    @Value("${hda.ratelimit.global-qps:500}")
    private int globalQps;

    private static final String GLOBAL_KEY = "hda:rl:global:qps";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (uri != null && uri.startsWith("/api/")
                && !rateLimiter.tryAcquire(GLOBAL_KEY, 1000L, globalQps)) {
            log.warn("全局限流触发: 超过 {} QPS, uri={}", globalQps, uri);
            writeReject(response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void writeReject(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");
        String body = "{\"code\":" + ResultCode.RATE_LIMIT_EXCEEDED.getCode()
                + ",\"message\":\"" + ResultCode.RATE_LIMIT_EXCEEDED.getMessage()
                + "\",\"data\":null}";
        response.getWriter().write(new String(body.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
    }
}
