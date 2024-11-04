package com.ssafy.dannae.global.exception.handler;

import com.ssafy.dannae.global.util.JwtTokenProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private final Map<Long, List<WebSocketSession>> gameRoomSessions = new ConcurrentHashMap<>();
    private final WaitingRoomWebSocketHandler waitingRoomHandler;
    private final JwtTokenProvider jwtTokenProvider;

    public GameWebSocketHandler(WaitingRoomWebSocketHandler waitingRoomHandler, JwtTokenProvider jwtTokenProvider) {
        this.waitingRoomHandler = waitingRoomHandler;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long roomId = getRoomIdFromSession(session);
        String token = getTokenFromSession(session);
        String nickname = getNicknameFromSession(session);

        if (!jwtTokenProvider.validateToken(token) || !waitingRoomHandler.isPlayerInWaitingRoom(roomId, jwtTokenProvider.getPlayerIdFromToken(token))) {
            session.sendMessage(new TextMessage("{\"type\": \"error\", \"event\": \"invalid_token\", \"message\": \"잘못된 토큰이거나 대기실에 입장한 사용자만 게임에 참여할 수 있습니다.\"}"));
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        List<WebSocketSession> sessions = gameRoomSessions.computeIfAbsent(roomId, k -> new CopyOnWriteArrayList<>());
        sessions.add(session);
        session.sendMessage(new TextMessage("{\"type\": \"enter\", \"event\": \"join_game\", \"message\": \"" + nickname + "님이 게임에 연결되었습니다.\"}"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        Long roomId = getRoomIdFromSession(session);
        String playerId = jwtTokenProvider.getPlayerIdFromToken(getTokenFromSession(session));
        String nickname = getNicknameFromSession(session);
        String payload = message.getPayload();

        String chatMessage = String.format("{\"type\": \"chat\", \"event\": \"message\", \"nickname\": \"%s\", \"playerId\": \"%s\", \"message\": \"%s\"}", nickname, playerId, payload);
        broadcastToRoom(roomId, chatMessage);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws IOException {
        Long roomId = getRoomIdFromSession(session);
        String nickname = getNicknameFromSession(session);
        List<WebSocketSession> sessions = gameRoomSessions.get(roomId);

        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                gameRoomSessions.remove(roomId);
            } else {
                broadcastToRoom(roomId, String.format("{\"type\": \"leave\", \"event\": \"disconnect\", \"message\": \"%s님이 게임을 나갔습니다.\"}", nickname));
            }
        }
    }

    private void broadcastToRoom(Long roomId, String message) throws IOException {
        List<WebSocketSession> sessions = gameRoomSessions.get(roomId);
        if (sessions != null) {
            for (WebSocketSession session : sessions) {
                session.sendMessage(new TextMessage(message));
            }
        }
    }

    private Long getRoomIdFromSession(WebSocketSession session) {
        String query = session.getUri().getQuery();
        return query != null && query.contains("roomId=") ? Long.valueOf(query.split("roomId=")[1].split("&")[0]) : null;
    }

    private String getTokenFromSession(WebSocketSession session) {
        List<String> tokenHeaders = session.getHandshakeHeaders().get("Authorization");
        if (tokenHeaders != null && !tokenHeaders.isEmpty()) {
            return tokenHeaders.get(0).replace("Bearer ", ""); // "Bearer "를 제거하고 토큰만 추출
        }
        return null;
    }

    private String getNicknameFromSession(WebSocketSession session) {
        String query = session.getUri().getQuery();
        return query != null && query.contains("nickname=") ? query.split("nickname=")[1].split("&")[0] : null;
    }
}
