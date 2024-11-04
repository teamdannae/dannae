package com.ssafy.dannae.global.exception.handler;

import com.ssafy.dannae.domain.player.entity.PlayerAuthorization;
import com.ssafy.dannae.domain.player.service.PlayerCommandService;
import com.ssafy.dannae.domain.player.service.PlayerQueryService;
import com.ssafy.dannae.domain.player.service.dto.PlayerDto;
import com.ssafy.dannae.domain.room.exception.NoRoomException;
import com.ssafy.dannae.domain.room.service.RoomQueryService;
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

import static com.ssafy.dannae.domain.player.entity.PlayerAuthorization.creator;
import static com.ssafy.dannae.domain.player.entity.PlayerAuthorization.player;
import static com.ssafy.dannae.domain.player.entity.PlayerStatus.nonready;

@Component
public class WaitingRoomWebSocketHandler extends TextWebSocketHandler {

    private static final int MAX_ROOM_CAPACITY = 4;
    private final Map<Long, List<WebSocketSession>> waitingRoomSessions = new ConcurrentHashMap<>();
    private final JwtTokenProvider jwtTokenProvider;
    private final PlayerQueryService playerQueryService;
    private final PlayerCommandService playerCommandService;
    private final RoomQueryService roomQueryService;

    public WaitingRoomWebSocketHandler(JwtTokenProvider jwtTokenProvider, PlayerQueryService playerQueryService, RoomQueryService roomQueryService, PlayerCommandService playerCommandService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.playerQueryService = playerQueryService;
        this.roomQueryService = roomQueryService;
        this.playerCommandService = playerCommandService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long roomId = getRoomIdFromSession(session);
        String token = getTokenFromSession(session);
        String nickname = getNicknameFromSession(session);

        if (roomId == null || !roomQueryService.existsById(roomId)) {
            throw new NoRoomException("존재하지 않는 방입니다.");
        }
        // 일반 유저 입장
        if (token == null || token.isEmpty()) {
            handleGeneralPlayerEntry(session, roomId, nickname);
        } else {
            String playerId = jwtTokenProvider.getPlayerIdFromToken(token);
            handleCreatorEntry(session, roomId, playerId);
        }
    }

    private void handleCreatorEntry(WebSocketSession session, Long roomId, String playerId) throws IOException {
        List<WebSocketSession> sessions = waitingRoomSessions.computeIfAbsent(roomId, k -> new CopyOnWriteArrayList<>());
        sessions.add(session);
        session.sendMessage(new TextMessage("{\"type\": \"enter\", \"event\": \"creator\", \"message\": \"방장으로 대기실에 입장했습니다.\"}"));
    }

    private void handleGeneralPlayerEntry(WebSocketSession session, Long roomId, String nickname) throws IOException {
        List<WebSocketSession> sessions = waitingRoomSessions.computeIfAbsent(roomId, k -> new CopyOnWriteArrayList<>());

        if (sessions.size() >= MAX_ROOM_CAPACITY) {
            session.sendMessage(new TextMessage("{\"type\": \"error\", \"message\": \"인원 초과로 방에 입장할 수 없습니다.\"}"));
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        PlayerDto playerDto = playerQueryService.createPlayer(PlayerDto.builder()
                .roomId(roomId)
                .score(0L)
                .status(nonready)
                .authorization(player)
                .nickname(nickname)
                .image(0)
                .build());

        String playerToken = jwtTokenProvider.createToken(roomId.toString(), playerDto.playerId().toString());

        sessions.add(session);
        session.sendMessage(new TextMessage("{\"type\": \"enter\", \"event\": \"player\", \"token\": \"" + playerToken + "\", \"message\": \"" + nickname + "님이 대기실에 들어왔습니다.\"}"));
        broadcastToRoom(roomId, "{\"type\": \"enter\", \"event\": \"player\", \"message\": \"" + nickname + "님이 대기실에 들어왔습니다.\"}");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        Long roomId = getRoomIdFromSession(session);
        String payload = message.getPayload();
        String nickname = getNicknameFromSession(session);

        String chatMessage = String.format("{\"type\": \"chat\", \"nickname\": \"%s\", \"message\": \"%s\"}", nickname, payload);
        broadcastToRoom(roomId, chatMessage);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws IOException {
        Long roomId = getRoomIdFromSession(session);
        String nickname = getNicknameFromSession(session); // 닉네임 가져오기
        String playerId = jwtTokenProvider.getPlayerIdFromToken(getTokenFromSession(session));
        List<WebSocketSession> sessions = waitingRoomSessions.get(roomId);

        if (sessions != null) {
            sessions.remove(session);

            // 현재 나가는 사용자가 방장인지 확인
            PlayerDto playerDto = playerQueryService.findPlayerById(Long.valueOf(playerId));
            if (playerDto.authorization() == creator) {
                if (!sessions.isEmpty()) {
                    // 대기실에 남아있는 첫 번째 사용자에게 방장 권한 위임
                    WebSocketSession newCreatorSession = sessions.get(0);
                    String newCreatorToken = getTokenFromSession(newCreatorSession);
                    Long newCreatorId = Long.parseLong(jwtTokenProvider.getPlayerIdFromToken(newCreatorToken));

                    playerCommandService.updateAuthorization(newCreatorId); // 방장 권한 업데이트

                    String newCreatorNickname = getNicknameFromSession(newCreatorSession);

                    newCreatorSession.sendMessage(new TextMessage(
                            "{\"type\": \"enter\", \"event\": \"creator_change\", \"message\": \"" + newCreatorNickname + "님이 방장이 되었습니다.\"}"
                    ));

                    broadcastToRoom(roomId, String.format("{\"type\": \"enter\", \"event\": \"creator_change\", \"message\": \"%s님이 방장이 되었습니다.\"}", newCreatorNickname));
                } else {
                    // 마지막 사용자가 나가면 세션과 대기실 목록에서 방 제거 및 종료 처리
                    waitingRoomSessions.remove(roomId);
                    session.close(CloseStatus.NORMAL);
                    return;
                }
            }

            broadcastToRoom(roomId, String.format("{\"type\": \"leave\", \"event\": \"player\", \"message\": \"%s님이 나갔습니다.\"}", nickname));
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
        return query != null && query.contains("nickname=") ? query.split("nickname=")[1].split("&")[0] : null;
    }

    public boolean isPlayerInWaitingRoom(Long roomId, String playerId) {
        List<WebSocketSession> sessions = waitingRoomSessions.get(roomId);
        if (sessions == null) {
            return false;
        }
        return sessions.stream()
                .anyMatch(session -> {
                    String token = getTokenFromSession(session);
                    String tokenPlayerId = jwtTokenProvider.getPlayerIdFromToken(token);
                    return playerId.equals(tokenPlayerId);
                });
    }

    public List<WebSocketSession> getRoomSessions(Long roomId) {
        return waitingRoomSessions.getOrDefault(roomId, new CopyOnWriteArrayList<>());
    }
}