package com.ssafy.dannae.global.exception.handler;

import com.ssafy.dannae.domain.player.entity.PlayerStatus;
import com.ssafy.dannae.domain.player.service.PlayerCommandService;
import com.ssafy.dannae.domain.player.service.PlayerQueryService;
import com.ssafy.dannae.domain.player.service.dto.PlayerDto;
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
public class SentenceGameWebSocketHandler extends TextWebSocketHandler {

    private final Map<Long, List<WebSocketSession>> gameRoomSessions = new ConcurrentHashMap<>();
    private final WaitingRoomWebSocketHandler waitingRoomHandler;
    private final JwtTokenProvider jwtTokenProvider;
    private final PlayerCommandService playerCommandService;
    private final PlayerQueryService playerQueryService;

    public SentenceGameWebSocketHandler(WaitingRoomWebSocketHandler waitingRoomHandler, JwtTokenProvider jwtTokenProvider, PlayerCommandService playerCommandService, PlayerQueryService playerQueryService) {
        this.waitingRoomHandler = waitingRoomHandler;
        this.jwtTokenProvider = jwtTokenProvider;
        this.playerCommandService=playerCommandService;
        this.playerQueryService= playerQueryService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long roomId = getRoomIdFromSession(session);
        String token = getTokenFromSession(session);
        String playerId = jwtTokenProvider.getPlayerIdFromToken(token);
        PlayerDto dto = playerQueryService.findPlayerById(Long.parseLong(playerId));
        String nickname = dto.nickname();
        int image = dto.image();

        if (!jwtTokenProvider.validateToken(token) || !waitingRoomHandler.isPlayerInWaitingRoom(roomId, playerId)) {
            session.sendMessage(new TextMessage("{\"type\": \"error\", \"event\": \"invalid_token\", \"message\": \"잘못된 토큰이거나 대기실에 입장한 사용자만 게임에 참여할 수 있습니다.\"}"));
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        List<WebSocketSession> sessions = gameRoomSessions.computeIfAbsent(roomId, k -> new CopyOnWriteArrayList<>());
        sessions.add(session);

        playerCommandService.updateStatus(Long.parseLong(playerId), PlayerStatus.playing);

        session.sendMessage(new TextMessage("{\"type\": \"enter\", \"event\": \"join_game\", \"message\": \"" + nickname + "님이 게임에 연결되었습니다.\", \"playerId\": \"" + playerId + "\", \"nickname\": \"" + nickname + "\", \"image\": " + image + ", \"status\": \"playing\"}"));

        String statusUpdateMessage = String.format("{\"type\": \"status_update\", \"playerId\": \"%s\", \"status\": \"playing\"}", playerId);
        broadcastToRoom(roomId, statusUpdateMessage);
    }


    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        Long roomId = getRoomIdFromSession(session);
        String playerId = jwtTokenProvider.getPlayerIdFromToken(getTokenFromSession(session));
        PlayerDto dto = playerQueryService.findPlayerById(Long.parseLong(playerId));
        String nickname = dto.nickname();
        int image = dto.image();
        String payload = message.getPayload();

        String chatMessage = String.format("{\"type\": \"chat\", \"event\": \"message\", \"nickname\": \"%s\", \"playerId\": \"%s\", \"message\": \"%s\", \"image\": %d}", nickname, playerId, payload, image);
        broadcastToRoom(roomId, chatMessage);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws IOException {
        Long roomId = getRoomIdFromSession(session);
        String playerId = jwtTokenProvider.getPlayerIdFromToken(getTokenFromSession(session));
        PlayerDto dto = playerQueryService.findPlayerById(Long.parseLong(playerId));
        String nickname = dto.nickname();
        int image = dto.image();

        List<WebSocketSession> sessions = gameRoomSessions.get(roomId);

        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                gameRoomSessions.remove(roomId);
            } else {
                broadcastToRoom(roomId, String.format("{\"type\": \"leave\", \"event\": \"disconnect\", \"message\": \"%s님이 게임을 나갔습니다.\", \"playerId\": \"%s\", \"nickname\": \"%s\", \"image\": %d}", nickname, playerId, nickname, image));
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
        String query = session.getUri().getQuery();
        if (query != null && query.contains("token=")) {
            return query.split("token=")[1].split("&")[0];
        }
        return null;
    }

}
