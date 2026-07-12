package com.medcare.hda.websocket;

import com.medcare.hda.security.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class DoctorConsultWebSocketHandler extends TextWebSocketHandler {

    private final JwtUtil jwtUtil;
    private final DoctorConsultNotifier notifier;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String token = tokenOf(session);
        Claims claims = StringUtils.hasText(token) ? jwtUtil.parseToken(token) : null;
        if (claims == null || jwtUtil.isRefreshToken(claims)) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("invalid token"));
            return;
        }
        Long userId = claims.get("userId", Long.class);
        if (userId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("invalid user"));
            return;
        }
        session.getAttributes().put("userId", userId);
        notifier.register(userId, session);
        session.sendMessage(new TextMessage("{\"type\":\"CONNECTED\",\"data\":true}"));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object userId = session.getAttributes().get("userId");
        if (userId instanceof Long id) {
            notifier.unregister(id, session);
        }
    }

    private String tokenOf(WebSocketSession session) {
        if (session.getUri() == null) {
            return null;
        }
        return UriComponentsBuilder.fromUri(session.getUri())
                .build()
                .getQueryParams()
                .getFirst("token");
    }
}
