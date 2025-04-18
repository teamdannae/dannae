package com.ssafy.dannae.global.exception.handler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.ssafy.dannae.domain.player.entity.PlayerStatus;
import com.ssafy.dannae.domain.player.service.PlayerCommandService;
import com.ssafy.dannae.domain.player.service.PlayerQueryService;
import com.ssafy.dannae.domain.player.service.dto.PlayerDto;
import com.ssafy.dannae.domain.room.entity.Room;
import com.ssafy.dannae.domain.room.entity.RoomStatus;
import com.ssafy.dannae.domain.room.exception.NoRoomException;
import com.ssafy.dannae.domain.room.service.RoomCommandService;
import com.ssafy.dannae.domain.room.service.RoomQueryService;
import com.ssafy.dannae.global.util.JwtTokenProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class WaitingRoomWebSocketHandler extends TextWebSocketHandler {

    private static final int MAX_ROOM_CAPACITY = 4;
    private final Map<Long, List<WebSocketSession>> waitingRoomSessions = new ConcurrentHashMap<>();
    private final Map<WebSocketSession, String> sessionTokenMap = new ConcurrentHashMap<>();
    private final Map<Long, WebSocketSession> roomCreatorMap = new ConcurrentHashMap<>(); // 각 방의 현재 방장 세션 추적
    private final JwtTokenProvider jwtTokenProvider;
    private final PlayerQueryService playerQueryService;
    private final RoomQueryService roomQueryService;
    private final RoomCommandService roomCommandService;
    private final PlayerCommandService playerCommandService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long roomId = getRoomIdFromSession(session);
        String token = getTokenFromSession(session);

        Room room = roomQueryService.findById(roomId)
                .orElseThrow(() -> new NoRoomException("방을 찾을 수 없습니다."));

        // 현재 방의 세션 리스트 가져오기
        List<WebSocketSession> sessions = waitingRoomSessions.computeIfAbsent(roomId, k -> new CopyOnWriteArrayList<>());

        // 방 인원 제한 확인
        if (sessions.size() >= MAX_ROOM_CAPACITY) {
            session.sendMessage(new TextMessage("{\"type\": \"error\", \"errorCode\": \"ROOM_FULL\", \"message\": \"방 인원이 최대치에 도달했습니다.\"}"));
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        // 토큰 유효성 검사
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            session.sendMessage(new TextMessage("{\"type\": \"error\", \"errorCode\": \"INVALID_TOKEN\", \"message\": \"유효하지 않은 토큰입니다.\"}"));
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        if (room.getStatus().equals(RoomStatus.PLAYING)) {
            session.sendMessage(new TextMessage("{\"type\": \"error\", \"errorCode\": \"GAME_IN_PROGRESS\", \"message\": \"현재 게임이 진행 중인 방입니다. 대기실에 들어올 수 없습니다.\"}"));
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        sessions.add(session);
        sessionTokenMap.put(session, token);

        int playerCount = sessions.size();
        roomCommandService.updatePlayerCount(roomId, (long) playerCount);

        WebSocketSession creatorSession = roomCreatorMap.get(roomId);
        if (creatorSession == null || !creatorSession.isOpen()) {
            // 방장이 없으면 새로운 방장을 할당
            assignNewCreator(sessions, roomId);
        }

        // 새로 입장한 사용자의 PlayerDto 가져오기
        String playerId = getPlayerIdFromSession(session);
        PlayerDto dto = playerQueryService.findPlayerById(Long.parseLong(playerId));
        playerCommandService.updateStatus(Long.valueOf(playerId), PlayerStatus.nonready);
        String nickname = dto.nickname();
        int image = dto.image();

        // 방장인 경우 roomCreatorMap에 등록
        if (room.getCreator().equals(Long.parseLong(playerId))) {
            roomCreatorMap.put(roomId, session);
        }

        Long creatorId = room.getCreator();

        // 현재 대기실에 있는 플레이어 목록 생성
        StringBuilder playerListMessage = new StringBuilder("{\"type\": \"current_players\", \"players\": [");
        for (WebSocketSession s : sessions) {
            String existingToken = getTokenFromSession(s);
            String existingPlayerId = getPlayerIdFromSession(s);
            long existingPlayerLongId = Long.parseLong(existingPlayerId);
            PlayerDto existingDto = playerQueryService.findPlayerById(existingPlayerLongId);

            playerListMessage.append("{\"playerId\": \"").append(existingPlayerId)
                    .append("\", \"nickname\": \"").append(existingDto.nickname())
                    .append("\", \"image\": ").append(existingDto.image())
                    .append(", \"status\": \"").append(existingDto.status())
                    .append("\", \"token\": \"").append(existingToken).append("\"},");

        }
        if (playerListMessage.charAt(playerListMessage.length() - 1) == ',') {
            playerListMessage.deleteCharAt(playerListMessage.length() - 1);
        }
        playerListMessage.append("], \"playerCount\": ").append(playerCount)
                .append(", \"creatorId\": \"").append(creatorId).append("\"}");

        // 새로 입장한 사용자에게 전체 플레이어 목록 전송
        session.sendMessage(new TextMessage(playerListMessage.toString()));

        // 다른 사용자들에게 입장 메시지 전송
        String enterMessage = String.format(
                "{\"type\": \"enter\", \"event\": \"rejoin_waiting\", \"message\": \"%s님이 대기실에 입장했습니다.\", \"playerId\": \"%s\", \"nickname\": \"%s\", \"image\": %d, \"playerCount\": %d, \"creatorId\": \"%s\"}",
                nickname, playerId, nickname, image, playerCount, creatorId
        );

        for (WebSocketSession s : sessions) {
            if (s != session) {
                s.sendMessage(new TextMessage(enterMessage));
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws IOException {
        try {
            Long roomId = getRoomIdFromSession(session);
            if (roomId == null) return;

            List<WebSocketSession> sessions = waitingRoomSessions.get(roomId);
            if (sessions == null || sessions.isEmpty()) return;

            String token = sessionTokenMap.remove(session);
            String playerId = (token != null) ? jwtTokenProvider.getPlayerIdFromToken(token) : null;
            playerCommandService.updateStatus(Long.valueOf(playerId),PlayerStatus.none);

            // 방장의 playerId 가져오기
            Room room = roomQueryService.findById(roomId)
                    .orElseThrow(() -> new NoRoomException("방을 찾을 수 없습니다."));
            Long creatorId = room.getCreator();

            sessions.remove(session);

            // 플레이어 수 업데이트
            int playerCount = getRoomPlayerCount(roomId);
            roomCommandService.updatePlayerCount(roomId, (long) playerCount);

            // 방장이 나갔다면 새 방장 할당 및 알림
            if (creatorId.equals(Long.parseLong(playerId))) {
                assignNewCreator(sessions, roomId);
                creatorId = roomQueryService.findById(roomId).orElseThrow().getCreator(); // 새 방장 정보 가져오기
                String newCreatorMessage = String.format(
                        "{\"type\": \"creator_change\", \"message\": \"%s님이 새로운 방장이 되었습니다.\", \"creatorId\": \"%s\", \"playerCount\": %d}",
                        playerQueryService.findPlayerById(creatorId).nickname(), creatorId, playerCount
                );

                // 모든 사용자에게 방장 변경 메시지 전송
                broadcastToRoom(roomId, newCreatorMessage);
            }

            if (sessions.isEmpty()) {
                waitingRoomSessions.remove(roomId);
                return;
            }

            // 나가는 사용자에 대한 알림 메시지 생성
            PlayerDto playerDto = playerQueryService.findPlayerById(Long.parseLong(playerId));
            String nickname = playerDto.nickname();

            if (nickname != null && playerId != null) {
                String leaveMessage = String.format(
                        "{\"type\": \"leave\", \"event\": \"player\", \"message\": \"%s님이 나갔습니다.\", \"playerId\": \"%s\", \"token\": \"%s\", \"nickname\": \"%s\", \"playerCount\": %d, \"creatorId\": \"%s\"}",
                        nickname, playerId, token, nickname, playerCount, creatorId
                );
                broadcastToRoom(roomId, leaveMessage);
            }
        } catch (Exception e) {
            log.error("Error in afterConnectionClosed", e);
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

                // 방장의 playerId 업데이트
                roomCommandService.updateRoomCreator(roomId, newCreatorId);

                // roomCreatorMap 업데이트
                roomCreatorMap.put(roomId, newCreatorSession);
                System.out.println("새 방장 등록됨: roomId=" + roomId + ", playerId=" + newCreatorId);
            }
        } catch (Exception e) {
            System.err.println("Error in assignNewCreator: " + e.getMessage());
            e.printStackTrace();
        }
    }


    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        Long roomId = getRoomIdFromSession(session);
        String payload = message.getPayload();

        // JSON 파싱
        JSONObject jsonMessage = new JSONObject(payload);
        String type = jsonMessage.getString("type");

        if ("start_game".equals(type)) {

            String playerId = jsonMessage.getString("playerId");

            // 요청한 사용자가 방장인지 확인
            Room room = roomQueryService.findById(roomId)
                    .orElseThrow(() -> new NoRoomException("방을 찾을 수 없습니다."));

            if (!room.getCreator().equals(Long.parseLong(playerId))) {
                session.sendMessage(new TextMessage("{\"type\": \"error\", \"errorCode\": \"NOT_ROOM_CREATOR\", \"message\": \"게임 시작 권한이 없습니다.\"}"));
                return;
            }

            onGameStartButtonClicked(roomId, session);
        } else if ("chat".equals(type)) {

            String playerId = getPlayerIdFromSession(session);
            PlayerDto playerDto = playerQueryService.findPlayerById(Long.parseLong(playerId));

            String chatMessage = String.format(
                    "{\"type\": \"chat\", \"nickname\": \"%s\", \"message\": \"%s\", \"playerId\": \"%s\", \"image\": %d}",
                    playerDto.nickname(), jsonMessage.getString("message") , playerId, playerDto.image()
            );

            broadcastToRoom(roomId, chatMessage);
        } else {

            session.sendMessage(new TextMessage("{\"type\": \"error\", \"errorCode\": \"UNKNOWN_TYPE\", \"message\": \"알 수 없는 메시지 타입입니다.\"}"));
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

    private String getPlayerIdFromSession(WebSocketSession session) {
        String token = sessionTokenMap.get(session);
        return token != null ? jwtTokenProvider.getPlayerIdFromToken(token) : null;
    }

    public int getRoomPlayerCount(Long roomId) {
        List<WebSocketSession> sessions = waitingRoomSessions.get(roomId);
        return sessions != null ? sessions.size() : 0;
    }

    public void broadcastPlayerStatusUpdate(Long roomId, Long playerId, PlayerStatus status) {

        String message = String.format(
                "{\"type\": \"status_update\", \"playerId\": \"%s\", \"status\": \"%s\"}",
                playerId, status
        );

        List<WebSocketSession> sessions = waitingRoomSessions.get(roomId);
        if (sessions != null) {
            for (WebSocketSession session : sessions) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    log.error("Failed to send status update message", e);
                }
            }
        }

        checkAllPlayersReadyAndNotify(roomId);
    }

    private void checkAllPlayersReadyAndNotify(Long roomId) {
        List<WebSocketSession> sessions = waitingRoomSessions.get(roomId);

        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        boolean allReady = sessions.stream()
                .map(this::getPlayerIdFromSession)
                .map(Long::valueOf)
                .map(playerQueryService::findPlayerById)
                .allMatch(dto -> {
                    boolean isReady = dto.status() == PlayerStatus.ready;
                    System.out.println("Player " + dto.playerId() + " status is ready: " + isReady);
                    return isReady;
                });


        if (allReady) {
            Room room = roomQueryService.findById(roomId)
                    .orElseThrow(() -> new NoRoomException("방을 찾을 수 없습니다."));

            String readyMessage = String.format(
                    "{\"type\": \"game_start_ready\", \"creatorId\": \"%s\", \"message\": \"모든 플레이어가 준비되었습니다. 게임을 시작할 수 있습니다.\", \"playerCount\": %d}",
                    room.getCreator(), sessions.size()
            );

            // Step 5: 방장에게 메시지 전송 시도
            WebSocketSession creatorSession = roomCreatorMap.get(roomId);
            if (creatorSession != null) {
                try {
                    creatorSession.sendMessage(new TextMessage(readyMessage));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("No creator session found for roomId " + roomId);
            }
        } else {
            System.out.println("Not all players in roomId " + roomId + " are ready");
        }
    }


    public void startGame(Long roomId) throws IOException {
        List<WebSocketSession> sessions = waitingRoomSessions.get(roomId);
        if (sessions != null) {
            // 방 정보 가져오기
            Room room = roomQueryService.findById(roomId)
                    .orElseThrow(() -> new NoRoomException("방을 찾을 수 없습니다."));

            // 게임 시작 메시지 생성
            roomCommandService.updateStatus(roomId);
            String startGameMessage = String.format(
                    "{\"type\": \"game_start\", \"message\": \"게임이 시작되었습니다!\", \"room\": {\"id\": \"%d\", \"title\": \"%s\", \"mode\": \"%s\", \"release\": %b, \"code\": \"%s\", \"playerCount\": %d}}",
                    room.getId(), room.getTitle(), room.getMode(), room.getRelease(), room.getCode(), room.getPlayerCount()
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

        if (roomCreatorMap.get(roomId) == session) {
            startGame(roomId); // 방장일 경우 게임 시작
        } else {
            session.sendMessage(new TextMessage("{\"type\": \"error\", \"message\": \"게임 시작 권한이 없습니다.\"}"));
        }
    }

}
