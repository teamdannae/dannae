package com.ssafy.dannae.global.exception.handler;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.ssafy.dannae.domain.game.infinitegame.service.InfiniteGameCommandService;
import com.ssafy.dannae.domain.game.infinitegame.service.dto.InfiniteGameDto;
import com.ssafy.dannae.domain.player.entity.PlayerStatus;
import com.ssafy.dannae.domain.player.service.PlayerCommandService;
import com.ssafy.dannae.domain.player.service.PlayerQueryService;
import com.ssafy.dannae.domain.player.service.dto.PlayerDto;
import com.ssafy.dannae.domain.room.exception.NoRoomException;
import com.ssafy.dannae.domain.room.service.RoomQueryService;
import com.ssafy.dannae.global.util.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InfiniteGameWebSocketHandler extends TextWebSocketHandler {

    private final Map<Long, List<WebSocketSession>> gameRoomSessions = new ConcurrentHashMap<>();
    private final Map<Long, Queue<WebSocketSession>> turnOrder = new ConcurrentHashMap<>();
    private final Map<WebSocketSession, String> sessionPlayerIdMap = new ConcurrentHashMap<>();
    private final Map<Long, Set<String>> usedWords = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Map<Long, String> gameConsonantsMap = new ConcurrentHashMap<>();
    private final Map<Long, Integer> initialPlayerCount = new ConcurrentHashMap<>(); // 게임 시작 시 플레이어 수 저장
    private final Map<Long, Long> gameIdsMap = new ConcurrentHashMap<>();

    private final InfiniteGameCommandService infiniteGameCommandService;
    private final WaitingRoomWebSocketHandler waitingRoomHandler;
    private final JwtTokenProvider jwtTokenProvider;
    private final PlayerCommandService playerCommandService;
    private final PlayerQueryService playerQueryService;
    private final RoomQueryService roomQueryService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long roomId = getRoomIdFromSession(session);
        String token = getTokenFromSession(session);

        if (!isTokenValidAndPlayerInWaitingRoom(token, roomId, session)) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        String playerId = jwtTokenProvider.getPlayerIdFromToken(token);

        // 플레이어 정보를 초기화하고 게임 세션에 추가
        initializePlayerInGame(session, roomId, playerId);

        // 모든 플레이어가 접속한 경우에만 초성 힌트 및 게임 시작 처리
        if (areAllPlayersConnected(roomId)) {
            usedWords.putIfAbsent(roomId, new HashSet<>());

            // 게임 시작 시의 인원 수를 기록
            initialPlayerCount.put(roomId, gameRoomSessions.get(roomId).size());

            // turnOrder 초기화 및 게임 시작
            initializeTurnOrder(roomId);
            startGame(roomId);
        }
    }

    private void initializeTurnOrder(Long roomId) {
        List<WebSocketSession> sessions = gameRoomSessions.get(roomId);
        if (sessions == null || sessions.isEmpty()) return;

        Queue<WebSocketSession> initialTurnOrder = new LinkedList<>(sessions);
        turnOrder.put(roomId, initialTurnOrder);

        System.out.println("Initialized turn order for room " + roomId + ": " + initialTurnOrder);
    }

    // 토큰 및 대기실에 있던 유저인지 확인.
    private boolean isTokenValidAndPlayerInWaitingRoom(String token, Long roomId, WebSocketSession session) throws IOException {
        if (!jwtTokenProvider.validateToken(token)) {
            sendErrorMessage(session, "invalid_token", "유효하지 않은 토큰입니다.");
            return false;
        }

        String playerId = jwtTokenProvider.getPlayerIdFromToken(token);
        // if (!waitingRoomHandler.isPlayerInWaitingRoom(roomId, playerId)) {
        //     sendErrorMessage(session, "not_in_waiting_room", "대기실에 입장한 사용자만 게임에 참여할 수 있습니다.");
        //     return false;
        // }

        return true;
    }

    private void initializePlayerInGame(WebSocketSession session, Long roomId, String playerId) throws IOException {
        // 플레이어 정보 가져오기
        PlayerDto dto = playerQueryService.findPlayerById(Long.parseLong(playerId));
        String nickname = dto.nickname();
        int image = dto.image();

        // 방의 세션 리스트에 플레이어 추가
        List<WebSocketSession> sessions = gameRoomSessions.computeIfAbsent(roomId, k -> new CopyOnWriteArrayList<>());
        sessionPlayerIdMap.put(session, playerId);
        sessions.add(session);

        // 플레이어 상태를 "playing"으로 업데이트
        playerCommandService.updateStatus(Long.parseLong(playerId), PlayerStatus.playing);

        // 플레이어에게 게임 참여 메시지 전송
        sendEnterGameMessage(session, playerId, nickname, image);

    }

    private boolean areAllPlayersConnected(Long roomId) {
        Long activePlayerCount = roomQueryService.findById(roomId)
            .orElseThrow(()-> new NoRoomException("no room exception"))
            .getPlayerCount();
        int currentSessionCount = gameRoomSessions.get(roomId).size();
        return currentSessionCount == activePlayerCount;
    }

    private void startGame(Long roomId) throws IOException {
        InfiniteGameDto gameDto = InfiniteGameDto.builder().roomId(roomId).build();
        InfiniteGameDto initialDto = infiniteGameCommandService.createInitial(gameDto);

        String initial = initialDto.initial();
        gameConsonantsMap.put(roomId, initial);
        Long infiniteGameId = initialDto.gameId();
        gameIdsMap.put(roomId, infiniteGameId);
        String message = String.format("{\"type\": \"infiniteGameStart\", \"initial\": \"%s\", \"infiniteGameId\": \"%s\"}", initial, infiniteGameId);

        broadcastToRoom(roomId, message);
        scheduler.schedule(() -> {
			try {
				startTurn(roomId, initialDto);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}, 5, TimeUnit.SECONDS);
    }

    private void startTurn(Long roomId, InfiniteGameDto initialDto) throws IOException {
        Queue<WebSocketSession> roomTurnOrder = turnOrder.get(roomId);
        System.out.println("Starting turn for room " + roomId + ". Turn order queue: " + roomTurnOrder);
        if (roomTurnOrder == null || roomTurnOrder.isEmpty()) {
            System.out.println("Ending game for room " + roomId + " - No players in turn order.");
            endGame(roomId);
            return;
        }

        WebSocketSession currentSession = roomTurnOrder.peek();
        if (currentSession != null && currentSession.isOpen()) {
            String currentPlayerId = sessionPlayerIdMap.get(currentSession);

            String turnInfoMessage = String.format(
                "{\"type\": \"turn_info\", \"message\": \"It's %s's turn!\", \"playerId\": \"%s\"}",
                currentPlayerId, currentPlayerId);
            broadcastToRoom(roomId, turnInfoMessage);

            try {
                String personalTurnMessage = "{\"type\": \"turn_start\", \"message\": \"It's your turn! You have 10 seconds to enter a message.\"}";
                currentSession.sendMessage(new TextMessage(personalTurnMessage));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // 제한시간 초과 처리
        scheduler.schedule(() -> {
            try {
                handleTurnTimeout(roomId, initialDto);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 15, TimeUnit.SECONDS);
    }


    private void handleTurnTimeout(Long roomId, InfiniteGameDto gameDto) {
        try {
            Queue<WebSocketSession> roomTurnOrder = turnOrder.get(roomId);
            if (roomTurnOrder == null || roomTurnOrder.isEmpty()) {
                return;  // 게임이 이미 종료된 경우
            }

            WebSocketSession currentSession = roomTurnOrder.peek();
            System.out.println("Handling turn timeout for room: " + roomId);

            if (currentSession != null) {
                handlePlayerElimination(currentSession, roomId, "timeout");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendEnterGameMessage(WebSocketSession session, String playerId, String nickname, int image) throws IOException {
        String message = String.format(
            "{\"type\": \"enter\", \"event\": \"join_game\", \"message\": \"%s님이 게임에 연결되었습니다.\", \"playerId\": \"%s\", \"nickname\": \"%s\", \"image\": %d, \"status\": \"playing\"}",
            nickname, playerId, nickname, image);
        session.sendMessage(new TextMessage(message));
    }

    private void sendErrorMessage(WebSocketSession session, String event, String message) throws IOException {
        session.sendMessage(new TextMessage(String.format("{\"type\": \"error\", \"event\": \"%s\", \"message\": \"%s\"}", event, message)));
    }

    private void handlePlayerElimination(WebSocketSession session, Long roomId, String reason) {
        String playerId = sessionPlayerIdMap.get(session);
        try {
            playerCommandService.updateStatus(Long.parseLong(playerId), PlayerStatus.end);

            String eliminationMessage = String.format(
                "{\"type\": \"elimination\", \"playerId\": \"%s\", \"reason\": \"%s\"}", playerId, reason
            );
            broadcastToRoom(roomId, eliminationMessage);

            turnOrder.get(roomId).remove(session);

            int remainingPlayers = turnOrder.get(roomId).size();
            int initialPlayers = initialPlayerCount.getOrDefault(roomId, 0);

            // 종료 조건
            if (initialPlayers == 1) { // 혼자 게임을 시작한 경우
                if (reason.equals("timeout") || reason.equals("오답입니다!")) {
                    System.out.println("Ending single-player game for room: " + roomId + " - Player failed.");
                    endGame(roomId);
                } else {
                    scheduler.schedule(() -> {
                        try {
                            moveToNextTurn(roomId);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }, 2, TimeUnit.SECONDS);
                }
            } else if (initialPlayers > 1 && remainingPlayers <= 1) { // 다인 게임에서 마지막 1명 남은 경우
                System.out.println("Ending game for room: " + roomId + " - Only one player remains.");
                endGame(roomId);
            } else {
                scheduler.schedule(() -> {
                    try {
                        moveToNextTurn(roomId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, 2, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void moveToNextTurn(Long roomId) {
        try {
            System.out.println("Moving to next turn for room: " + roomId);

            Queue<WebSocketSession> roomTurnOrder = turnOrder.get(roomId);
            if (roomTurnOrder == null || roomTurnOrder.isEmpty()) {
                System.out.println("Ending game for room: " + roomId + " - Turn order is empty.");
                endGame(roomId);
                return;
            }

            WebSocketSession currentSession = roomTurnOrder.poll();
            if (currentSession != null) {
                roomTurnOrder.offer(currentSession);
                System.out.println("Next turn assigned to session: " + sessionPlayerIdMap.get(currentSession));
            }

            int initialPlayers = initialPlayerCount.getOrDefault(roomId, 0);
            int remainingPlayers = roomTurnOrder.size();

            // 싱글 플레이어일 경우, 게임을 계속 반복
            if (initialPlayers == 1) {
                System.out.println("Single player game, continuing with the same player.");
                InfiniteGameDto nextGameDto = InfiniteGameDto.builder().roomId(roomId).build();
                startTurn(roomId, nextGameDto);
            }
            // 다인 게임인 경우 마지막 1명만 남으면 종료
            else if (remainingPlayers <= 1) {
                System.out.println("Ending game for room: " + roomId + " - Only one player is left.");
                endGame(roomId);
            } else {
                InfiniteGameDto nextGameDto = InfiniteGameDto.builder().roomId(roomId).build();
                startTurn(roomId, nextGameDto); // 다음 플레이어에게 턴을 넘깁니다.
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void endGame(Long roomId) throws IOException {
        broadcastToRoom(roomId, "{\"type\": \"game_end\", \"message\": \"Game over!\"}");
        gameRoomSessions.remove(roomId);
        turnOrder.remove(roomId);
        usedWords.remove(roomId);
        initialPlayerCount.remove(roomId); // 초기 플레이어 수 정보도 제거
        gameConsonantsMap.remove(roomId); // 저장된 초성 정보도 삭제
        gameIdsMap.remove(roomId); // 게임 ID 삭제
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        Long roomId = getRoomIdFromSession(session);
        String playerId = jwtTokenProvider.getPlayerIdFromToken(getTokenFromSession(session));
        String payload = message.getPayload();

        // JSON 메시지 파싱
        JSONObject jsonMessage = new JSONObject(payload);
        String type = jsonMessage.getString("type");

        // 답 입력 처리 (type이 "answer"일 때)
        if ("answer".equals(type)) {
            String answer = jsonMessage.getString("answer");

            processAnswer(session, roomId, Long.parseLong(playerId), answer);
        }
    }

    private void processAnswer(WebSocketSession session, Long roomId, Long playerId, String answer) throws IOException {
        Long gameId = gameIdsMap.get(roomId); // 저장된 게임 ID 가져오기

        // 채점 요청 DTO 생성
        InfiniteGameDto answerDto = InfiniteGameDto.builder()
            .roomId(roomId)
            .gameId(gameId)  // 해당 방의 게임 ID 가져오기
            .playerId(playerId)
            .initial(gameConsonantsMap.get(roomId))  // 해당 방의 초성 가져오기
            .word(answer)
            .build();

        // 채점 요청
        InfiniteGameDto result = infiniteGameCommandService.updateWord(answerDto);

        // reason에 대한 메시지를 생성: correct가 true이면 "정답입니다!" 메시지, false이면 result.meaning() 값
        String reasonMessage = result.correct() ? "정답입니다!" : (result.meaning() != null && !result.meaning().isEmpty() ? result.meaning().get(0) : "틀린 답변입니다.");

        // 결과에 따른 JSON 형식으로 메시지 생성
        String responseMessage = String.format(
            "{\"type\": \"answer_result\", \"data\": {\"correct\": %b, \"word\": \"%s\", \"reason\": \"%s\", \"difficulty\": %d}}",
            result.correct(),
            result.word(),
            reasonMessage,
            result.difficulty() != null ? result.difficulty() : null
        );

        // 방 전체에 정답 맞춘 플레이어 알림
        broadcastToRoom(roomId, responseMessage);

        if (result.correct()) {
            // 다음 턴으로 이동
            moveToNextTurn(roomId);
        } else{
            WebSocketSession currentSession = turnOrder.get(roomId).peek();

            if (currentSession != null) {
                handlePlayerElimination(currentSession, roomId, "오답입니다!");
            }
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
                broadcastToRoom(roomId, String.format("{\"type\": \"leave\", \"event\": \"disconnect\", \"message\": \"%s님이 게임을 나갔습니다.\", \"playerId\": \"%s\", \"nickname\": \"%s\"}", nickname, playerId, nickname));
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
        System.out.println(query);
        if (query != null && query.contains("token=")) {
            return query.split("token=")[1].split("&")[0];
        }
        return null;
    }
}
