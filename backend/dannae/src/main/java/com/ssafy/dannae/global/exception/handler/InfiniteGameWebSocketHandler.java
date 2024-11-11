package com.ssafy.dannae.global.exception.handler;

import java.io.IOException;
import java.util.HashMap;
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
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.ssafy.dannae.domain.game.infinitegame.service.InfiniteGameCommandService;
import com.ssafy.dannae.domain.game.infinitegame.service.dto.InfiniteGameDto;
import com.ssafy.dannae.domain.game.infinitegame.service.dto.SubmittedAnswer;
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
    private final Map<Long, SubmittedAnswer> submittedAnswers = new ConcurrentHashMap<>(); // SubmittedAnswer 필드 추가
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Map<Long, String> gameConsonantsMap = new ConcurrentHashMap<>();
    private final Map<Long, Integer> initialPlayerCount = new ConcurrentHashMap<>();
    private final Map<Long, Long> gameIdsMap = new ConcurrentHashMap<>();
    private final Map<String, String> playerNicknames = new HashMap<>(); // 사용자 ID와 닉네임 매핑
    private final Map<Long, Boolean> turnInProgress = new ConcurrentHashMap<>(); // 플래그 맵 추가

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
        initializePlayerInGame(session, roomId, playerId);

        if (areAllPlayersConnected(roomId)) {
            usedWords.putIfAbsent(roomId, new HashSet<>());
            initialPlayerCount.put(roomId, gameRoomSessions.get(roomId).size());
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

    private boolean isTokenValidAndPlayerInWaitingRoom(String token, Long roomId, WebSocketSession session) throws IOException {
        if (!jwtTokenProvider.validateToken(token)) {
            sendErrorMessage(session, "invalid_token", "유효하지 않은 토큰입니다.");
            return false;
        }

        String playerId = jwtTokenProvider.getPlayerIdFromToken(token);
        return true;
    }

    private void initializePlayerInGame(WebSocketSession session, Long roomId, String playerId) throws IOException {
        PlayerDto dto = playerQueryService.findPlayerById(Long.parseLong(playerId));
        String nickname = dto.nickname();
        int image = dto.image();

        // ID와 닉네임 매핑 저장
        sessionPlayerIdMap.put(session, playerId);
        playerNicknames.put(playerId, nickname); // 사용자 ID와 닉네임 저장

        List<WebSocketSession> sessions = gameRoomSessions.computeIfAbsent(roomId, k -> new CopyOnWriteArrayList<>());
        sessions.add(session);

        playerCommandService.updateStatus(Long.parseLong(playerId), PlayerStatus.playing);

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

        System.out.println("startTurn 시작 시 turnOrder: " + roomTurnOrder.stream()
            .map(s -> sessionPlayerIdMap.get(s))
            .collect(Collectors.joining(" -> ")));

        if (roomTurnOrder == null || roomTurnOrder.isEmpty()) {
            endGame(roomId);
            return;
        }

        WebSocketSession currentSession = roomTurnOrder.peek();
        if (currentSession != null && currentSession.isOpen()) {
            String currentPlayerId = sessionPlayerIdMap.get(currentSession);
            String nickname = playerNicknames.get(currentPlayerId);

            String turnInfoMessage = String.format(
                "{\"type\": \"turn_info\", \"message\": \"%s님의 턴입니다!\", \"playerId\": \"%s\"}",
                nickname, currentPlayerId);
            broadcastToRoom(roomId, turnInfoMessage);

            try {
                String personalTurnMessage = "{\"type\": \"turn_start\", \"message\": \"당신의 차례입니다. 10초 안에 정답을 입력해주세요!\"}";
                currentSession.sendMessage(new TextMessage(personalTurnMessage));
            } catch (IOException e) {
                e.printStackTrace();
            }

            turnInProgress.put(roomId, true); // 턴이 진행 중임을 표시

            scheduler.schedule(() -> {
                try {
                    handleTurnTimeoutOrCheckAnswer(roomId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 10, TimeUnit.SECONDS);
        }
    }

    private void processAnswer(WebSocketSession session, Long roomId, Long playerId, String answer) {
        System.out.println("Processing answer for playerId: " + playerId + " with answer: " + answer); // 디버깅 로그 추가
        submittedAnswers.put(roomId, new SubmittedAnswer(playerId, answer, session));
    }

    private void handleTurnTimeoutOrCheckAnswer(Long roomId) throws IOException {
        if (Boolean.FALSE.equals(turnInProgress.get(roomId))) {
            return; // 턴이 이미 진행 중이 아님
        }

        SubmittedAnswer submittedAnswer = submittedAnswers.get(roomId);
        Queue<WebSocketSession> roomTurnOrder = turnOrder.get(roomId);
        WebSocketSession currentSession = roomTurnOrder.peek();

        boolean isTimeout = submittedAnswer == null || submittedAnswer.getSession() != currentSession;

        if (isTimeout) {
            broadcastToRoom(roomId, "{\"type\": \"timeout\", \"message\": \"턴 종료!\"}");
            scheduler.schedule(() -> {
                handlePlayerElimination(currentSession, roomId, "시간 초과");
                moveToNextTurn(roomId);
            }, 2, TimeUnit.SECONDS);
        } else {
            String answer = submittedAnswer.getAnswer();
            Long playerId = submittedAnswer.getPlayerId();
            String initial = gameConsonantsMap.get(roomId);
            Long gameId = gameIdsMap.get(roomId);

            if (gameId == null || playerId == null) {
                System.out.println("Error: gameId 또는 playerId가 null입니다.");
                return;
            }

            try {
                InfiniteGameDto answerDto = InfiniteGameDto.builder()
                    .roomId(roomId)
                    .gameId(gameId)
                    .word(answer)
                    .playerId(playerId)
                    .initial(initial)
                    .build();

                InfiniteGameDto result = infiniteGameCommandService.updateWord(answerDto);

                System.out.println("Evaluating answer. Result: correct=" + result.correct() + ", word=" + result.word() + ", reason=" + (result.correct() ? "정답입니다!" : "틀린 답변입니다."));

                String resultMessage = String.format(
                    "{\"type\": \"answer_result\", \"correct\": %b, \"word\": \"%s\", \"reason\": \"%s\"}",
                    result.correct(), result.word(), result.correct() ? "정답입니다!" : result.meaning().get(0));

                broadcastToRoom(roomId, resultMessage);

                String nickname = playerNicknames.get(playerId.toString());

                if (result.correct()) {
                    String successMessage = String.format(
                        "{\"type\": \"success\", \"message\": \"%s님 정답입니다!\"}", nickname
                    );
                    broadcastToRoom(roomId, successMessage);

                    scheduler.schedule(() -> {
                        moveToNextTurn(roomId);
                    }, 2, TimeUnit.SECONDS);
                } else {
                    String failureMessage = String.format(
                        "{\"type\": \"failure\", \"message\": \"%s님 오답입니다.\"}", nickname
                    );
                    broadcastToRoom(roomId, failureMessage);

                    scheduler.schedule(() -> {
                        handlePlayerElimination(currentSession, roomId, "오답입니다!");
                        moveToNextTurn(roomId);
                    }, 2, TimeUnit.SECONDS);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        turnInProgress.put(roomId, false); // 턴 완료 표시
        submittedAnswers.remove(roomId);
    }

    private void handlePlayerElimination(WebSocketSession session, Long roomId, String reason) {
        String playerId = sessionPlayerIdMap.get(session);
        try {
            playerCommandService.updateStatus(Long.parseLong(playerId), PlayerStatus.end);

            String eliminationMessage = String.format(
                "{\"type\": \"elimination\", \"playerId\": \"%s\", \"reason\": \"%s\"}", playerId, reason
            );
            broadcastToRoom(roomId, eliminationMessage);

            Queue<WebSocketSession> roomTurnOrder = turnOrder.get(roomId);
            roomTurnOrder.remove(session);

            int remainingPlayers = roomTurnOrder.size();
            System.out.println("남은 플레이어 수: " + remainingPlayers);

            if (remainingPlayers <= 1) {
                endGame(roomId);
            } else {
                startTurn(roomId, InfiniteGameDto.builder().roomId(roomId).build());  // moveToNextTurn 대신 바로 startTurn 호출
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void moveToNextTurn(Long roomId) {
        if (Boolean.TRUE.equals(turnInProgress.get(roomId))) {
            System.out.println("턴이 이미 진행 중이라 moveToNextTurn 스킵");
            return;
        }

        try {
            Queue<WebSocketSession> roomTurnOrder = turnOrder.get(roomId);
            System.out.println("moveToNextTurn 시작 시 turnOrder: " + roomTurnOrder.stream()
                .map(s -> sessionPlayerIdMap.get(s))
                .collect(Collectors.joining(" -> ")));

            // 정답자를 큐의 맨 뒤로
            WebSocketSession currentSession = roomTurnOrder.poll();
            if (currentSession != null) {
                roomTurnOrder.offer(currentSession);
            }

            turnInProgress.put(roomId, true);
            startTurn(roomId, InfiniteGameDto.builder().roomId(roomId).build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void endGame(Long roomId) throws IOException {
        broadcastToRoom(roomId, "{\"type\": \"game_end\", \"message\": \"게임 종료!\"}");
        gameRoomSessions.remove(roomId);
        turnOrder.remove(roomId);
        usedWords.remove(roomId);
        initialPlayerCount.remove(roomId);
        gameConsonantsMap.remove(roomId);
        gameIdsMap.remove(roomId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        Long roomId = getRoomIdFromSession(session);
        String playerId = jwtTokenProvider.getPlayerIdFromToken(getTokenFromSession(session));
        String payload = message.getPayload();

        JSONObject jsonMessage = new JSONObject(payload);
        String type = jsonMessage.getString("type");

        if ("answer".equals(type)) {
            String answer = jsonMessage.getString("answer");
            processAnswer(session, roomId, Long.parseLong(playerId), answer);
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
