package com.medcare.hda.security;

import com.medcare.hda.entity.User;
import com.medcare.hda.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** JWT 认证过滤器：解析 token 并注入 SecurityContext */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final UserMapper userMapper;

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
                Long userId = claims.get("userId", Long.class);
                User user = userMapper.selectById(userId);
                if (user != null && (user.getStatus() == null || user.getStatus() == 0)) {
                    LoginUser loginUser = new LoginUser(user);
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
}
