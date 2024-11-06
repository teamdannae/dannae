package com.ssafy.dannae.global.exception.handler;

import com.ssafy.dannae.domain.player.entity.PlayerStatus;
import com.ssafy.dannae.domain.player.service.PlayerCommandService;
import com.ssafy.dannae.domain.player.service.PlayerQueryService;
import com.ssafy.dannae.domain.player.service.dto.PlayerDto;
import com.ssafy.dannae.domain.room.entity.Room;
import com.ssafy.dannae.domain.room.exception.NoRoomException;
import com.ssafy.dannae.domain.room.service.RoomQueryService;
import com.ssafy.dannae.global.util.JwtTokenProvider;
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

import static com.ssafy.dannae.domain.player.entity.PlayerAuthorization.player;
import static com.ssafy.dannae.domain.player.entity.PlayerStatus.nonready;

@Slf4j
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

        // 토큰 유효성 검사
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            session.sendMessage(new TextMessage("{\"type\": \"error\", \"message\": \"유효하지 않은 토큰입니다.\"}"));
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        for (String existingToken : sessionTokenMap.values()) {
            if (existingToken.equals(token)) {
                session.sendMessage(new TextMessage("{\"type\": \"error\", \"message\": \"이미 사용 중인 토큰입니다.\"}"));
                session.close(CloseStatus.POLICY_VIOLATION);
                return;
            }
        }

        List<WebSocketSession> sessions = waitingRoomSessions.computeIfAbsent(roomId, k -> new CopyOnWriteArrayList<>());
        sessions.add(session);
        sessionTokenMap.put(session, token);

        if (roomCreatorMap.get(roomId) == null) {
            roomCreatorMap.put(roomId, session); // 방장 설정
        }

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

    private void handleCreatorEntry(WebSocketSession session, Long roomId, String playerId) throws IOException {
        List<WebSocketSession> sessions = waitingRoomSessions.computeIfAbsent(roomId, k -> new CopyOnWriteArrayList<>());
        sessions.add(session);
        int playerCount = getRoomPlayerCount(roomId);

        PlayerDto playerDto = playerQueryService.findPlayerById(Long.valueOf(playerId));
        Integer image = (playerDto != null) ? playerDto.image() : 0;
        String nickname = (playerDto != null) ? playerDto.nickname() : "";

        String token = sessionTokenMap.get(session);

        session.sendMessage(new TextMessage(
                String.format("{\"type\": \"enter\", \"event\": \"creator\", \"message\": \"방장으로 대기실에 입장했습니다.\", \"playerId\": \"%s\", \"token\": \"%s\", \"nickname\": \"%s\", \"image\": %d, \"playerCount\": %d}",
                        playerId, token, nickname, image, playerCount)
        ));
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

        String playerToken = jwtTokenProvider.createToken(playerDto.playerId().toString());
        sessionTokenMap.put(session, playerToken);

        sessions.add(session);
        int playerCount = getRoomPlayerCount(roomId);

        StringBuilder playerListMessage = new StringBuilder("{\"type\": \"current_players\", \"players\": [");
        for (WebSocketSession s : sessions) {
            String token = sessionTokenMap.get(s);
            String existingPlayerId = getPlayerIdFromSession(s);
            String existingNickname = getNicknameFromSession(s);
            Integer existingImage = getImageFromSession(s);

            String authorization = roomCreatorMap.get(roomId) == s ? "creator" : "player";

            playerListMessage.append("{\"playerId\": \"").append(existingPlayerId)
                    .append("\", \"nickname\": \"").append(existingNickname)
                    .append("\", \"image\": ").append(existingImage)
                    .append(", \"token\": \"").append(token).append("\"")
                    .append(", \"authorization\": \"").append(authorization).append("\"},");
        }
        if (playerListMessage.charAt(playerListMessage.length() - 1) == ',') {
            playerListMessage.deleteCharAt(playerListMessage.length() - 1);
        }
        playerListMessage.append("], \"playerCount\": ").append(playerCount).append("}");

        session.sendMessage(new TextMessage(playerListMessage.toString()));

        String enterMessage = String.format(
                "{\"type\": \"enter\", \"event\": \"player\", \"message\": \"%s님이 대기실에 들어왔습니다.\", \"playerId\": \"%s\", \"nickname\": \"%s\", \"image\": %d, \"authorization\": \"player\", \"playerCount\": %d}",
                nickname, playerDto.playerId(), nickname, image, playerCount
        );

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

            String token = sessionTokenMap.remove(session);
            String playerId = (token != null) ? jwtTokenProvider.getPlayerIdFromToken(token) : null;

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
                        "{\"type\": \"leave\", \"event\": \"player\", \"message\": \"%s님이 나갔습니다.\", \"playerId\": \"%s\", \"token\": \"%s\", \"nickname\": \"%s\", \"playerCount\": %d}",
                        nickname, playerId, token, nickname, playerCount
                );
                broadcastToRoom(roomId, leaveMessage);
            }
        } catch (Exception e) {
            log.error("Error in afterConnectionClosed", e);
        }
    }

    // 방장 권한 양도
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

    public void broadcastPlayerStatusUpdate(Long playerId, PlayerStatus status) {
        String message = String.format(
                "{\"type\": \"status_update\", \"playerId\": \"%s\", \"status\": \"%s\"}",
                playerId, status
        );

        waitingRoomSessions.values().forEach(sessions ->
                sessions.forEach(session -> {
                    try {
                        session.sendMessage(new TextMessage(message));
                    } catch (IOException e) {
                        log.error("Failed to send status update message", e);
                    }
                })
        );
    }
    public void startGame(Long roomId) throws IOException {
        List<WebSocketSession> sessions = waitingRoomSessions.get(roomId);
        if (sessions != null) {
            // 방 정보 가져오기
            Room room = roomQueryService.findById(roomId)
                    .orElseThrow(() -> new NoRoomException("방을 찾을 수 없습니다."));

            // 게임 시작 메시지 생성
            String startGameMessage = String.format(
                    "{\"type\": \"game_start\", \"message\": \"게임이 시작되었습니다!\", \"room\": {\"id\": \"%d\", \"title\": \"%s\", \"mode\": \"%s\", \"release\": %b, \"code\": \"%s\", \"joinCount\": %d}}",
                    room.getId(), room.getTitle(), room.getMode(), room.getRelease(), room.getCode(), room.getJoinCount()
            );

            // 모든 세션에 게임 시작 메시지 전송
            for (WebSocketSession session : sessions) {
                session.sendMessage(new TextMessage(startGameMessage));
            }

            // 필요 시, 대기실 세션 목록 초기화 (게임 시작 후 대기실 비우기)
            waitingRoomSessions.remove(roomId);
        }
    }


    public void onGameStartButtonClicked(Long roomId, WebSocketSession session) throws IOException {
        // 요청한 사용자가 방장인지 확인
        if (roomCreatorMap.get(roomId) == session) {
            startGame(roomId); // 방장일 경우 게임 시작
        } else {
            session.sendMessage(new TextMessage("{\"type\": \"error\", \"message\": \"게임 시작 권한이 없습니다.\"}"));
        }
    }
}
