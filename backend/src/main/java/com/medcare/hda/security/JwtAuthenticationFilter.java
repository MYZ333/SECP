package com.medcare.hda.security;

import com.medcare.hda.common.ResultCode;
import com.medcare.hda.entity.User;
import com.medcare.hda.mapper.RoleMapper;
import com.medcare.hda.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** JWT 认证过滤器：解析 token、校验黑名单、比对 Redis 登录态并注入 SecurityContext */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String TOKEN_KEY_PREFIX = "hda:login:token:";
    private static final String BLACKLIST_PREFIX = "hda:token:blacklist:";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader(jwtProperties.getHeader());
        if (StringUtils.hasText(header) && header.startsWith(jwtProperties.getTokenPrefix())) {
            String token = header.substring(jwtProperties.getTokenPrefix().length());
            Claims claims = jwtUtil.parseToken(token);
            if (claims != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // 1) 黑名单校验：已登出的 token 立即失效
                String jti = claims.getId();
                if (jti != null && Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + jti))) {
                    writeError(response, ResultCode.TOKEN_INVALID);
                    return;
                }
                Long userId = claims.get("userId", Long.class);
                // 2) 单点登录校验：token 合法但与 Redis 当前登录态不一致 → 被挤下线
                Object saved = redisTemplate.opsForValue().get(TOKEN_KEY_PREFIX + userId);
                if (!Objects.equals(token, saved)) {
                    writeError(response, ResultCode.LOGIN_ELSEWHERE);
                    return;
                }
                User user = userMapper.selectById(userId);
                if (user != null && (user.getStatus() == null || user.getStatus() == 0)) {
                    LoginUser loginUser = new LoginUser(user, roleMapper.selectRoleCodesByUserId(userId));
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    loginUser, null, loginUser.getAuthorities());
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    /** 直接输出 JSON 错误（如被挤下线/黑名单），让前端拿到明确业务码而非笼统 401 */
    private void writeError(HttpServletResponse response, ResultCode rc) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        String body = "{\"code\":" + rc.getCode()
                + ",\"message\":\"" + rc.getMessage() + "\",\"data\":null}";
        response.getWriter().write(new String(body.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
    }
}
