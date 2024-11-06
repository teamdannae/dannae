package com.ssafy.dannae.global.exception.handler;

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

@Slf4j
@Component
public class AgainWaitingRoomWebSocketHandler extends TextWebSocketHandler {
    private final Map<Long, List<WebSocketSession>> waitingRoomSessions = new ConcurrentHashMap<>();
    private final Map<WebSocketSession, String> sessionTokenMap = new ConcurrentHashMap<>();
    private final Map<Long, WebSocketSession> roomCreatorMap = new ConcurrentHashMap<>(); // 각 방의 현재 방장 세션 추적
    private final RoomQueryService roomQueryService;
    private final JwtTokenProvider jwtTokenProvider;

    public AgainWaitingRoomWebSocketHandler(RoomQueryService roomQueryService, JwtTokenProvider jwtTokenProvider) {
        this.roomQueryService = roomQueryService;
        this.jwtTokenProvider = jwtTokenProvider;
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

        // 같은 토큰이 이미 세션에 존재하는지 확인
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

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws IOException {
        Long roomId = getRoomIdFromSession(session);
        List<WebSocketSession> sessions = waitingRoomSessions.get(roomId);

        if (sessions != null) {
            sessions.remove(session);
            String token = sessionTokenMap.remove(session);
            boolean isCreator = roomCreatorMap.get(roomId) == session;

            String nickname = getNicknameFromSession(session); // 닉네임 가져오기

            if (isCreator) {
                roomCreatorMap.remove(roomId);
                if (!sessions.isEmpty()) {
                    assignNewCreator(sessions, roomId);
                }
            }

            if (sessions.isEmpty()) {
                waitingRoomSessions.remove(roomId);
            } else {
                broadcastToRoom(roomId, String.format(
                        "{\"type\": \"leave\", \"event\": \"player_leave\", \"message\": \"%s님이 대기실에서 나갔습니다.\", \"nickname\": \"%s\", \"roomId\": \"%s\"}",
                        nickname, nickname, roomId
                ));
            }
        }
    }

    private void assignNewCreator(List<WebSocketSession> sessions, Long roomId) {
        if (sessions.isEmpty()) return;

        try {
            WebSocketSession newCreatorSession = sessions.get(0);
            String newCreatorToken = sessionTokenMap.get(newCreatorSession);

            if (newCreatorToken != null && !newCreatorToken.isEmpty()) {
                roomCreatorMap.put(roomId, newCreatorSession);

                String newCreatorNickname = getNicknameFromSession(newCreatorSession);
                int playerCount = sessions.size();
                String creatorChangeMessage = String.format(
                        "{\"type\": \"enter\", \"event\": \"creator_change\", \"message\": \"%s님이 방장이 되었습니다.\", \"playerId\": \"%s\", \"nickname\": \"%s\", \"playerCount\": %d}",
                        newCreatorNickname, newCreatorToken, newCreatorNickname, playerCount
                );

                newCreatorSession.sendMessage(new TextMessage(creatorChangeMessage));
                for (WebSocketSession otherSession : sessions) {
                    if (!otherSession.equals(newCreatorSession)) {
                        otherSession.sendMessage(new TextMessage(creatorChangeMessage));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error in assignNewCreator", e);
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
}
