package com.medcare.hda.aspect;

import com.medcare.hda.annotation.Idempotent;
import com.medcare.hda.common.ResultCode;
import com.medcare.hda.exception.BusinessException;
import com.medcare.hda.util.WebUtil;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;

/** 幂等切面：原子消费请求头里的幂等 token，重复提交拒绝 */
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotentAspect {

    public static final String TOKEN_PREFIX = "hda:idempotent:";

    private final StringRedisTemplate redisTemplate;
    private DefaultRedisScript<Long> script;

    @PostConstruct
    public void init() {
        script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("lua/idempotent_check.lua")));
        script.setResultType(Long.class);
    }

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint pjp, Idempotent idempotent) throws Throwable {
        HttpServletRequest request = WebUtil.currentRequest();
        String token = request == null ? null : request.getHeader(idempotent.header());
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(ResultCode.IDEMPOTENT_TOKEN_MISSING);
        }
        Long ok = redisTemplate.execute(script, Collections.singletonList(TOKEN_PREFIX + token));
        if (ok == null || ok == 0L) {
            throw new BusinessException(ResultCode.REPEAT_SUBMIT.getCode(), idempotent.message());
        }
        return pjp.proceed();
    }
}
