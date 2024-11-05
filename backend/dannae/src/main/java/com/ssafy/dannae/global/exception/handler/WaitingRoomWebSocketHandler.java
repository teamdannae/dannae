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
    private final Map<WebSocketSession, String> sessionTokenMap = new ConcurrentHashMap<>();
    private final Map<Long, WebSocketSession> roomCreatorMap = new ConcurrentHashMap<>(); // 각 방의 현재 방장 세션 추적
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
        Integer image = getImageFromSession(session);

        if (roomId == null || !roomQueryService.existsById(roomId)) {
            throw new NoRoomException("존재하지 않는 방입니다.");
        }

        if (token != null && !token.isEmpty()) {
            String playerId = jwtTokenProvider.getPlayerIdFromToken(token);
            handleCreatorEntry(session, roomId, playerId);
            sessionTokenMap.put(session, token);
            roomCreatorMap.put(roomId, session); // 방장 세션 등록
        } else {
            handleGeneralPlayerEntry(session, roomId, nickname, image);
        }
    }

    private void handleCreatorEntry(WebSocketSession session, Long roomId, String playerId) throws IOException {
        List<WebSocketSession> sessions = waitingRoomSessions.computeIfAbsent(roomId, k -> new CopyOnWriteArrayList<>());
        sessions.add(session);
        int playerCount = getRoomPlayerCount(roomId);

        PlayerDto playerDto = playerQueryService.findPlayerById(Long.valueOf(playerId));
        Integer image = (playerDto != null) ? playerDto.image() : 0;
        String nickname = (playerDto != null) ? playerDto.nickname() : "";

        session.sendMessage(new TextMessage("{\"type\": \"enter\", \"event\": \"creator\", \"message\": \"방장으로 대기실에 입장했습니다.\", \"playerId\": \"" + playerId + "\", \"nickname\": \"" + nickname + "\", \"image\": " + image + ", \"playerCount\": " + playerCount + "}"));
    }

    private void handleGeneralPlayerEntry(WebSocketSession session, Long roomId, String nickname, Integer image) throws IOException {
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
                .image(image)
                .build());

        String playerToken = jwtTokenProvider.createToken(roomId.toString(), playerDto.playerId().toString());
        sessionTokenMap.put(session, playerToken);

        sessions.add(session);
        int playerCount = getRoomPlayerCount(roomId);

        StringBuilder playerListMessage = new StringBuilder("{\"type\": \"current_players\", \"players\": [");
        for (WebSocketSession s : sessions) {
            String existingPlayerId = getPlayerIdFromSession(s);
            String existingNickname = getNicknameFromSession(s);
            Integer existingImage = getImageFromSession(s);

            String authorization = roomCreatorMap.get(roomId) == s ? "creator" : "player";

            playerListMessage.append("{\"playerId\": \"").append(existingPlayerId)
                    .append("\", \"nickname\": \"").append(existingNickname)
                    .append("\", \"image\": ").append(existingImage)
                    .append(", \"authorization\": \"").append(authorization).append("\"},");
        }
        if (playerListMessage.charAt(playerListMessage.length() - 1) == ',') {
            playerListMessage.deleteCharAt(playerListMessage.length() - 1);
        }
        playerListMessage.append("], \"playerCount\": ").append(playerCount).append("}");

        session.sendMessage(new TextMessage(playerListMessage.toString()));

        String enterMessage = String.format("{\"type\": \"enter\", \"event\": \"player\", \"message\": \"%s님이 대기실에 들어왔습니다.\", \"image\": %d, \"playerId\": \"%s\", \"nickname\": \"%s\", \"authorization\": \"player\", \"playerCount\": %d}",
                nickname, image, playerDto.playerId(), nickname, playerCount);

        // 입장 메시지를 자기 자신을 제외한 다른 플레이어에게만 전송
        for (WebSocketSession s : sessions) {
            if (s != session) {
                s.sendMessage(new TextMessage(enterMessage));
            }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        Long roomId = getRoomIdFromSession(session);
        String payload = message.getPayload();
        String nickname = getNicknameFromSession(session);
        String playerId = getPlayerIdFromSession(session);

        String chatMessage = String.format("{\"type\": \"chat\", \"nickname\": \"%s\", \"message\": \"%s\", \"playerId\": \"%s\"}", nickname, payload, playerId);
        broadcastToRoom(roomId, chatMessage);
    }
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws IOException {
        try {
            Long roomId = getRoomIdFromSession(session);
            if (roomId == null) return;

            List<WebSocketSession> sessions = waitingRoomSessions.get(roomId);
            if (sessions == null || sessions.isEmpty()) return;

            boolean isCreator = roomCreatorMap.get(roomId) == session;

            sessions.remove(session);

            // 토큰 매핑에서 session 제거
            String token = sessionTokenMap.remove(session);
            String playerId = null;

            // token이 있다면 playerId를 가져옴
            if (token != null) {
                playerId = jwtTokenProvider.getPlayerIdFromToken(token);
            }

            if (isCreator) {
                roomCreatorMap.remove(roomId);
                if (!sessions.isEmpty()) {
                    assignNewCreator(sessions, roomId);
                }
            }

            if (sessions.isEmpty()) {
                waitingRoomSessions.remove(roomId);
                return;
            }

            String nickname = getNicknameFromSession(session);
            int playerCount = getRoomPlayerCount(roomId);

            if (nickname != null && playerId != null) {
                String leaveMessage = String.format(
                        "{\"type\": \"leave\", \"event\": \"player\", \"message\": \"%s님이 나갔습니다.\", \"playerId\": \"%s\", \"nickname\": \"%s\", \"playerCount\": %d}",
                        nickname, playerId, nickname, playerCount
                );
                broadcastToRoom(roomId, leaveMessage);
            }

            for (WebSocketSession remainingSession : sessions) {
                String remainingPlayerId = getPlayerIdFromSession(remainingSession);
                String remainingNickname = getNicknameFromSession(remainingSession);
                Integer remainingImage = getImageFromSession(remainingSession);
                String authorization = roomCreatorMap.get(roomId) == remainingSession ? "creator" : "player";

                remainingSession.sendMessage(new TextMessage(
                        String.format("{\"type\": \"update\", \"event\": \"player_list\", \"playerId\": \"%s\", \"nickname\": \"%s\", \"image\": %d, \"authorization\": \"%s\", \"playerCount\": %d}",
                                remainingPlayerId, remainingNickname, remainingImage, authorization, playerCount)
                ));
            }

        } catch (Exception e) {
            System.err.println("Error in afterConnectionClosed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void assignNewCreator(List<WebSocketSession> sessions, Long roomId) {
        if (sessions.isEmpty()) return;

        try {
            WebSocketSession newCreatorSession = sessions.get(0);
            String newCreatorToken = sessionTokenMap.get(newCreatorSession);

            if (newCreatorToken != null && !newCreatorToken.isEmpty()) {
                String playerIdFromToken = jwtTokenProvider.getPlayerIdFromToken(newCreatorToken);
                Long newCreatorId = Long.parseLong(playerIdFromToken);

                playerCommandService.updateAuthorization(newCreatorId);
                roomCreatorMap.put(roomId, newCreatorSession);

                String newCreatorNickname = getNicknameFromSession(newCreatorSession);
                if (newCreatorNickname != null) {
                    int playerCount = getRoomPlayerCount(roomId);
                    String creatorChangeMessage = String.format(
                            "{\"type\": \"enter\", \"event\": \"creator_change\", \"message\": \"%s님이 방장이 되었습니다.\", \"newCreatorToken\": \"%s\", \"playerId\": \"%s\", \"nickname\": \"%s\", \"playerCount\": %d}",
                            newCreatorNickname, newCreatorToken, playerIdFromToken, newCreatorNickname, playerCount
                    );

                    try {
                        newCreatorSession.sendMessage(new TextMessage(creatorChangeMessage));
                        String otherMessage = String.format(
                                "{\"type\": \"enter\", \"event\": \"creator_change\", \"message\": \"%s님이 방장이 되었습니다.\", \"playerId\": \"%s\", \"nickname\": \"%s\", \"playerCount\": %d}",
                                newCreatorNickname, playerIdFromToken, newCreatorNickname, playerCount
                        );
                        for (WebSocketSession otherSession : sessions) {
                            if (!otherSession.equals(newCreatorSession)) {
                                otherSession.sendMessage(new TextMessage(otherMessage));
                            }
                        }
                    } catch (IOException e) {
                        System.err.println("Failed to send creator change message: " + e.getMessage());
                    }
                }
            } else {
                System.err.println("New creator token not found in sessionTokenMap");
            }
        } catch (Exception e) {
            System.err.println("Error in assignNewCreator: " + e.getMessage());
            e.printStackTrace();
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
        String token = sessionTokenMap.get(session);
        return token != null ? jwtTokenProvider.getPlayerIdFromToken(token) : null;
    }

    public boolean isPlayerInWaitingRoom(Long roomId, String playerId) {
        List<WebSocketSession> sessions = waitingRoomSessions.get(roomId);
        if (sessions == null) {
            return false;
        }
        return sessions.stream()
                .anyMatch(session -> {
                    String token = sessionTokenMap.get(session);
                    if (token == null || token.isEmpty()) {
                        return false;
                    }
                    String tokenPlayerId = jwtTokenProvider.getPlayerIdFromToken(token);
                    return playerId.equals(tokenPlayerId);
                });
    }

    public List<WebSocketSession> getRoomSessions(Long roomId) {
        return waitingRoomSessions.getOrDefault(roomId, new CopyOnWriteArrayList<>());
    }

    public int getRoomPlayerCount(Long roomId) {
        List<WebSocketSession> sessions = waitingRoomSessions.get(roomId);
        return sessions != null ? sessions.size() : 0;
    }
}
