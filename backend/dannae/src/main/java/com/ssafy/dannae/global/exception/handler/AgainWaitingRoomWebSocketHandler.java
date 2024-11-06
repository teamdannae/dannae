package com.ssafy.dannae.global.exception.handler;

import com.ssafy.dannae.domain.room.exception.NoRoomException;
import com.ssafy.dannae.domain.room.service.RoomQueryService;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Component
public class AgainWaitingRoomWebSocketHandler extends TextWebSocketHandler {
    private final Map<Long, List<WebSocketSession>> waitingRoomSessions = new ConcurrentHashMap<>();
    private final RoomQueryService roomQueryService;

    public AgainWaitingRoomWebSocketHandler(RoomQueryService roomQueryService) {
        this.roomQueryService = roomQueryService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long roomId = getRoomIdFromSession(session);
        String token = getTokenFromSession(session);
        String nickname = getNicknameFromSession(session);
        Integer image = getImageFromSession(session);

        if (roomId == null || !roomQueryService.existsById(roomId)) {
            throw new NoRoomException("존재하지 않는 방입니다.");
        }

        List<WebSocketSession> sessions = waitingRoomSessions.computeIfAbsent(roomId, k -> new CopyOnWriteArrayList<>());
        sessions.add(session);
        int playerCount = sessions.size();

        // 현재 대기실에 있는 플레이어 목록 생성
        StringBuilder playerListMessage = new StringBuilder("{\"type\": \"current_players\", \"players\": [");
        for (WebSocketSession s : sessions) {
            String existingToken = getTokenFromSession(s);
            String existingPlayerId = getPlayerIdFromSession(s);
            String existingNickname = getNicknameFromSession(s);
            Integer existingImage = getImageFromSession(s);

            playerListMessage.append("{\"playerId\": \"").append(existingPlayerId)
                    .append("\", \"nickname\": \"").append(existingNickname)
                    .append("\", \"image\": ").append(existingImage)
                    .append(", \"token\": \"").append(existingToken).append("\"},");
        }
        if (playerListMessage.charAt(playerListMessage.length() - 1) == ',') {
            playerListMessage.deleteCharAt(playerListMessage.length() - 1);
        }
        playerListMessage.append("], \"playerCount\": ").append(playerCount).append("}");

        // 새로 입장한 사용자에게 전체 플레이어 목록 전송
        session.sendMessage(new TextMessage(playerListMessage.toString()));

        // 입장 메시지를 다른 플레이어에게도 전달
        String enterMessage = String.format(
                "{\"type\": \"enter\", \"event\": \"rejoin_waiting\", \"message\": \"%s님이 대기실에 재입장했습니다.\", \"playerId\": \"%s\", \"nickname\": \"%s\", \"image\": %d, \"playerCount\": %d}",
                nickname, "playerId_placeholder", nickname, image, playerCount
        );

        for (WebSocketSession s : sessions) {
            if (s != session) {
                s.sendMessage(new TextMessage(enterMessage));
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws IOException {
        Long roomId = getRoomIdFromSession(session);
        List<WebSocketSession> sessions = waitingRoomSessions.get(roomId);

        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                waitingRoomSessions.remove(roomId);
            } else {
                broadcastToRoom(roomId, String.format("{\"type\": \"leave\", \"event\": \"player_leave\", \"message\": \"사용자가 대기실에서 나갔습니다.\", \"roomId\": \"%s\"}", roomId));
            }
        }
    }

    private void broadcastToRoom(Long roomId, String message) throws IOException {
        List<WebSocketSession> sessions = waitingRoomSessions.get(roomId);
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

    private String getNicknameFromSession(WebSocketSession session) {
        String query = session.getUri().getQuery();
        return query != null && query.contains("nickname=") ? query.split("nickname=")[1].split("&")[0] : "Unknown";
    }

    private Integer getImageFromSession(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null && query.contains("image=")) {
            try {
                return Integer.parseInt(query.split("image=")[1].split("&")[0]);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    private String getPlayerIdFromSession(WebSocketSession session) {
        String token = getTokenFromSession(session);
        return token != null ? token : "Unknown"; // 토큰에서 playerId를 추출하는 방법이 정의되어 있다면 수정
    }
}
