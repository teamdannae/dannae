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

        // 토큰 검증
        if (!jwtTokenProvider.validateToken(token) || !waitingRoomHandler.isPlayerInWaitingRoom(roomId, token)) {
            session.sendMessage(new TextMessage("잘못된 토큰이거나 대기실에 입장한 사용자만 게임에 참여할 수 있습니다."));
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        gameRoomSessions.computeIfAbsent(roomId, k -> new CopyOnWriteArrayList<>()).add(session);
        session.sendMessage(new TextMessage("게임에 연결되었습니다."));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        Long roomId = getRoomIdFromSession(session);
        broadcastToRoom(roomId, message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long roomId = getRoomIdFromSession(session);
        List<WebSocketSession> sessions = gameRoomSessions.get(roomId);

        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                gameRoomSessions.remove(roomId);
            }
        }
    }

    private void handleGameExit(Long roomId) throws IOException {
        broadcastToRoom(roomId, "게임이 종료되었습니다. 대기실로 돌아갑니다.");
        List<WebSocketSession> sessions = gameRoomSessions.get(roomId);

        if (sessions != null) {
            for (WebSocketSession session : sessions) {
                session.close(CloseStatus.NORMAL);
            }
            gameRoomSessions.remove(roomId);
        }

        for (WebSocketSession session : waitingRoomHandler.getRoomSessions(roomId)) {
            session.sendMessage(new TextMessage("대기실로 돌아왔습니다. 새로운 게임을 기다려 주세요."));
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
        String query = session.getUri().getQuery();
        return query != null && query.contains("token=") ? query.split("token=")[1].split("&")[0] : null;
    }
}
