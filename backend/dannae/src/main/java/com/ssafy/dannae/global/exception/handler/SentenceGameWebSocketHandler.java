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
import java.util.concurrent.*;

@Component
public class SentenceGameWebSocketHandler extends TextWebSocketHandler {

    private final Map<Long, List<WebSocketSession>> gameRoomSessions = new ConcurrentHashMap<>();
    private final WaitingRoomWebSocketHandler waitingRoomHandler;
    private final JwtTokenProvider jwtTokenProvider;
    private final PlayerCommandService playerCommandService;
    private final PlayerQueryService playerQueryService;
    private final int roundTimeLimit = 20;
    private final int roundWaitTime = 5;
    private final Map<Long, Map<String, Boolean>> roundPlayerStatus = new ConcurrentHashMap<>();
    private final Map<WebSocketSession, String> sessionTokenMap = new ConcurrentHashMap<>();
    private int currentRound = 1;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

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
        sessionTokenMap.put(session, token);

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

        String payload = message.getPayload();
        Map<String, Boolean> playerStatus = roundPlayerStatus.get(roomId);

        // 플레이어가 이미 메시지를 보냈다면 무시
        if (playerStatus != null && playerStatus.get(playerId)) {
            session.sendMessage(new TextMessage("{\"type\": \"error\", \"message\": \"이미 메시지를 보냈습니다.\"}"));
            return;
        }

        playerStatus.put(playerId, true);
        String chatMessage = String.format("{\"type\": \"chat\", \"event\": \"message\", \"playerId\": \"%s\", \"message\": \"%s\"}", playerId, payload);
        // 자기 자신에게만 메시지 전송
        session.sendMessage(new TextMessage(chatMessage));
        if (checkIfAllPlayersSentMessages(roomId)) {
            endRound(roomId);
        }
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

    public void startNewRound(Long roomId) {
        Map<String, Boolean> playerStatus = new ConcurrentHashMap<>();
        List<WebSocketSession> sessions = gameRoomSessions.get(roomId);

        if (sessions != null) {
            sessions.forEach(session -> {
                String playerId = getPlayerIdFromSession(session);
                playerStatus.put(playerId, false); // 각 플레이어의 메시지 전송 상태 초기화
            });
        }

        roundPlayerStatus.put(roomId, playerStatus);

        // 타이머 시작
        scheduler.schedule(() -> {
            if (!checkIfAllPlayersSentMessages(roomId)) {
                endRound(roomId); // 시간 초과로 라운드 종료
            }
        }, roundTimeLimit, TimeUnit.SECONDS);

        broadcastToRoom(roomId, "{\"type\": \"round_start\", \"round\": \"" + currentRound + "\", \"message\": \"새로운 라운드가 시작되었습니다!\"}");
    }


    private void endRound(Long roomId) {
        // 여기서 채점로직 넣을 예정
        broadcastToRoom(roomId, "{\"type\": \"round_end\", \"message\": \"라운드가 종료되었습니다.\"}");
    }

    private String getPlayerIdFromSession(WebSocketSession session) {
        String token = sessionTokenMap.get(session);
        return token != null ? jwtTokenProvider.getPlayerIdFromToken(token) : null;
    }

    // 모든 플레이어가 메시지를 보냈는지 확인
    private boolean checkIfAllPlayersSentMessages(Long roomId) {
        Map<String, Boolean> playerStatus = roundPlayerStatus.get(roomId);
        return playerStatus != null && playerStatus.values().stream().allMatch(sent -> sent);
    }

    private void broadcastToRoom(Long roomId, String message) {
        List<WebSocketSession> sessions = gameRoomSessions.get(roomId);
        if (sessions != null) {
            for (WebSocketSession session : sessions) {
                try {
                    session.sendMessage(new TextMessage(message)); // 메시지를 각 세션에 전송
                } catch (IOException e) {
                    System.err.println("Failed to send message to session " + session.getId() + ": " + e.getMessage());
                }
            }
        } else {
            System.out.println("No active sessions found for roomId " + roomId); // 방이 존재하지 않는 경우 로그 출력
        }
    }


}
