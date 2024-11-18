package com.ssafy.dannae.global.exception.handler;

import java.io.IOException;
import java.util.ArrayList;
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
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
import com.ssafy.dannae.domain.player.service.dto.PlayerIdListDto;
import com.ssafy.dannae.domain.rank.service.RankCommandService;
import com.ssafy.dannae.domain.room.entity.Room;
import com.ssafy.dannae.domain.room.exception.NoRoomException;
import com.ssafy.dannae.domain.room.service.RoomCommandService;
import com.ssafy.dannae.domain.room.service.RoomQueryService;
import com.ssafy.dannae.global.util.JwtTokenProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class InfiniteGameWebSocketHandler extends TextWebSocketHandler {

    private final Map<Long, List<WebSocketSession>> gameRoomSessions = new ConcurrentHashMap<>();
    private final Map<Long, Queue<WebSocketSession>> turnOrder = new ConcurrentHashMap<>();
    private final Map<WebSocketSession, String> sessionPlayerIdMap = new ConcurrentHashMap<>();
    private final Map<Long, Set<String>> usedWords = new ConcurrentHashMap<>();
    private final Map<Long, List<SubmittedAnswer>> submittedAnswers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(
        Runtime.getRuntime().availableProcessors()
    );
    private final Map<Long, ScheduledFuture<?>> turnTimeoutTasks = new ConcurrentHashMap<>();

    private final Map<Long, String> gameConsonantsMap = new ConcurrentHashMap<>();
    private final Map<Long, Long> gameIdsMap = new ConcurrentHashMap<>();
    private final Map<String, String> playerNicknames = new HashMap<>(); // 사용자 ID와 닉네임 매핑
    private final Map<Long, Boolean> turnInProgress = new ConcurrentHashMap<>(); // 플래그 맵 추가
    private final Map<Long, Boolean> isSinglePlayerGame = new ConcurrentHashMap<>();
    private final Map<Long, AtomicBoolean> gameStartedMap = new ConcurrentHashMap<>(); // 방별 게임 시작 플래그
    private final Map<Long, Set<String>> activePlayersMap = new ConcurrentHashMap<>();

    private final InfiniteGameCommandService infiniteGameCommandService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PlayerCommandService playerCommandService;
    private final PlayerQueryService playerQueryService;
    private final RoomQueryService roomQueryService;
    private final RoomCommandService roomCommandService;
    private final RankCommandService rankCommandService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long roomId = getRoomIdFromSession(session);
        String token = getTokenFromSession(session);
        String playerId = jwtTokenProvider.getPlayerIdFromToken(token);

        if (!isTokenValidAndPlayerInWaitingRoom(token, roomId, session)) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        boolean alreadyConnected = gameRoomSessions.getOrDefault(roomId, new ArrayList<>()).stream()
            .anyMatch(s -> {
                String existingPlayerId = sessionPlayerIdMap.get(s);
                return playerId.equals(existingPlayerId) && s.isOpen();
            });

        if (alreadyConnected) {
            sendErrorMessage(session, "duplicate_connection", "이미 게임에 접속 중입니다.");
            session.close();
            return;
        }

        initializePlayerInGame(session, roomId, playerId);

        gameStartedMap.putIfAbsent(roomId, new AtomicBoolean(false)); // 초기화

        if (areAllPlayersConnected(roomId) && gameStartedMap.get(roomId).compareAndSet(false, true)) {
            usedWords.putIfAbsent(roomId, new HashSet<>());
            startGame(roomId);
        }
    }

    private void initializeTurnOrder(Long roomId) {
        List<WebSocketSession> sessions = gameRoomSessions.get(roomId);
        if (sessions == null || sessions.isEmpty()) return;

        // 열려있는 세션만 포함하도록 필터링
        List<WebSocketSession> activeSessions = sessions.stream()
            .filter(WebSocketSession::isOpen)
            .collect(Collectors.toList());

        Queue<WebSocketSession> initialTurnOrder = new LinkedList<>(activeSessions);
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

        Set<String> activePlayers = activePlayersMap.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet());
        activePlayers.add(playerId);

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

        Long playerCount = roomQueryService.findById(roomId)
            .orElseThrow(() -> new NoRoomException("no room exception"))
            .getPlayerCount();

        isSinglePlayerGame.put(roomId, playerCount == 1);

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
                // 5초 후 실제 게임 시작 전에 플레이어 수 다시 확인
                List<WebSocketSession> activeSessions = gameRoomSessions.get(roomId);
                if (activeSessions != null && !activeSessions.isEmpty()) {
                    initializeTurnOrder(roomId);
                    startTurn(roomId, initialDto);
                } else {
                    // 플레이어가 부족하면 게임 종료
                    endGame(roomId);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, 5, TimeUnit.SECONDS);
    }

    private void cancelAllScheduledTasks(Long roomId) {
        ScheduledFuture<?> timeoutTask = turnTimeoutTasks.get(roomId);
        if (timeoutTask != null) {
            timeoutTask.cancel(true);
            turnTimeoutTasks.remove(roomId);
        }
        turnInProgress.remove(roomId);
    }

    // startTurn 메서드 수정: 스케줄러 저장 및 턴 제한 시간 스케줄러 추가
    private void startTurn(Long roomId, InfiniteGameDto initialDto) throws IOException {
        cancelAllScheduledTasks(roomId);

        Queue<WebSocketSession> roomTurnOrder = turnOrder.get(roomId);

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

            String personalTurnMessage = "{\"type\": \"turn_start\", \"message\": \"당신의 차례입니다. 10초 안에 정답을 입력해주세요!\"}";
            currentSession.sendMessage(new TextMessage(personalTurnMessage));

            turnInProgress.put(roomId, true);

            // 제한 시간 스케줄러 설정 및 저장
            ScheduledFuture<?> timeoutTask = scheduler.schedule(() -> {
                try {
                    handleTurnTimeoutOrCheckAnswer(roomId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 10, TimeUnit.SECONDS);

            turnTimeoutTasks.put(roomId, timeoutTask); // 스케줄러 저장
        }
    }

    // processAnswer 메서드 수정: 답변 제출 시 스케줄러 취소
    private void processAnswer(WebSocketSession session, Long roomId, Long playerId, String answer) {
        System.out.println("Processing answer for playerId: " + playerId + " with answer: " + answer);

        // 사용자가 답을 제출했으므로 제한 시간 스케줄러 취소
        ScheduledFuture<?> timeoutTask = turnTimeoutTasks.get(roomId);
        if (timeoutTask != null && !timeoutTask.isDone()) {
            timeoutTask.cancel(false);
        }

        // 방의 답변 리스트 가져오기 (없으면 새로 생성)
        List<SubmittedAnswer> answers = submittedAnswers.computeIfAbsent(roomId, k -> new CopyOnWriteArrayList<>());
        answers.add(new SubmittedAnswer(playerId, answer, session));

        // 이후 정답 판정 로직 수행 (예: handleTurnTimeoutOrCheckAnswer 로 호출하여 다음 턴으로 이동)
        try {
            handleTurnTimeoutOrCheckAnswer(roomId);  // 즉시 정답 판정 및 다음 턴으로 이동
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleTurnTimeoutOrCheckAnswer(Long roomId) throws IOException {
        if (Boolean.FALSE.equals(turnInProgress.get(roomId))) {
            return; // 턴이 이미 진행 중이 아님
        }

        List<SubmittedAnswer> answers = submittedAnswers.get(roomId);
        Queue<WebSocketSession> roomTurnOrder = turnOrder.get(roomId);
        WebSocketSession currentSession = roomTurnOrder.peek();

        boolean isTimeout = answers == null || answers.isEmpty() ||
            answers.stream()
                .noneMatch(answer -> answer.getSession() == currentSession);

        if (isTimeout) {
            broadcastToRoom(roomId, "{\"type\": \"timeout\", \"message\": \"턴 종료!\"}");
            scheduler.schedule(() -> {
                handlePlayerElimination(currentSession, roomId, "시간 초과");
                moveToNextTurn(roomId);
            }, 2, TimeUnit.SECONDS);
        } else {
            SubmittedAnswer lastAnswer = answers.stream()
                .filter(answer -> answer.getSession() == currentSession)
                .reduce((first, second) -> second)
                .get();

            String answer = lastAnswer.getAnswer();
            Long playerId = lastAnswer.getPlayerId();
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
                    "{\"type\": \"answer_result\", \"correct\": %b, \"word\": \"%s\", \"reason\": \"%s\", \"difficulty\": %d}",
                    result.correct(), answer, result.correct() ? "정답입니다!" : result.meaning().get(0), result.difficulty());

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
                        "{\"type\": \"failure\", \"message\": \"%s님 %s\"}", nickname, result.meaning().get(0)
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

            Set<String> activePlayers = activePlayersMap.get(roomId);
            if (activePlayers != null) {
                activePlayers.remove(playerId);

                // 남은 활성 플레이어들의 점수 업데이트
                for (String survivorId : activePlayers) {
                    playerCommandService.updateScore(Long.parseLong(survivorId), 100);
                }
            }

            String eliminationMessage = String.format(
                "{\"type\": \"elimination\", \"playerId\": \"%s\", \"reason\": \"%s\"}", playerId, reason
            );
            broadcastToRoom(roomId, eliminationMessage);

            Queue<WebSocketSession> roomTurnOrder = turnOrder.get(roomId);
            List<WebSocketSession> activeSessions = gameRoomSessions.get(roomId);
            roomTurnOrder.remove(session);

            long actualActivePlayers = roomTurnOrder.stream()
                .filter(s -> activeSessions.contains(s))
                .count();

            System.out.println("실제 남은 플레이어 수: " + actualActivePlayers);

            // 1인용 게임인 경우는 actualActivePlayers가 0이 되었을 때만 종료
            // 다인용 게임인 경우는 actualActivePlayers가 1 이하가 되었을 때 종료
            boolean shouldEndGame = Boolean.TRUE.equals(isSinglePlayerGame.get(roomId))
                ? actualActivePlayers < 1
                : actualActivePlayers <= 1;

            if (shouldEndGame) {
                endGame(roomId);
            } else {
                startTurn(roomId, InfiniteGameDto.builder().roomId(roomId).build());
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
            List<WebSocketSession> activeSessions = gameRoomSessions.get(roomId);

            if (roomTurnOrder == null || roomTurnOrder.isEmpty()) {
                System.out.println("Room " + roomId + "의 턴 순서가 없거나 비어있음 - 게임 종료 처리");
                endGame(roomId);
                return;
            }

            long actualActivePlayers = roomTurnOrder.stream()
                .filter(s -> activeSessions.contains(s))
                .count();

            boolean shouldEndGame = Boolean.TRUE.equals(isSinglePlayerGame.get(roomId))
                ? actualActivePlayers < 1
                : actualActivePlayers <= 1;

            if (shouldEndGame) {
                endGame(roomId);
                return;
            }

            System.out.println("moveToNextTurn 시작 시 turnOrder: " + roomTurnOrder.stream()
                .map(s -> sessionPlayerIdMap.get(s))
                .collect(Collectors.joining(" -> ")));

            // 현재 플레이어를 큐에서 제거
            WebSocketSession currentSession = roomTurnOrder.poll();

            // 현재 세션이 activeSessions에 있는 경우에만 다시 큐에 추가
            if (currentSession != null && activeSessions.contains(currentSession)) {
                roomTurnOrder.offer(currentSession);
            }

            // 다음 차례의 플레이어가 activeSessions에 없다면 건너뛰기
            while (!roomTurnOrder.isEmpty() && !activeSessions.contains(roomTurnOrder.peek())) {
                roomTurnOrder.poll();
            }

            turnInProgress.put(roomId, true);
            startTurn(roomId, InfiniteGameDto.builder().roomId(roomId).build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized void endGame(Long roomId) throws IOException {

        // 이미 게임이 종료되었는지 확인 (맵에서 제거되었는지 체크)
        if (!gameRoomSessions.containsKey(roomId)) {
            return;  // 이미 종료된 게임이면 리턴
        }

        if (Boolean.TRUE.equals(turnInProgress.get(roomId))) {
            // 턴 종료 처리를 기다리거나 강제 종료할지 결정
            ScheduledFuture<?> timeoutTask = turnTimeoutTasks.get(roomId);
            if (timeoutTask != null) {
                timeoutTask.cancel(true);
            }
            turnInProgress.put(roomId, false);
        }

        // 게임 종료 메시지 전송
        broadcastToRoom(roomId, "{\"type\": \"game_end\", \"message\": \"게임 종료!\"}");

        try {
            // room status 변경 (한 번만 실행되도록 보장)
            roomCommandService.updateStatus(roomId);
            gameStartedMap.get(roomId).set(false);

            List<Long> playerIdList = sessionPlayerIdMap.values().stream()
                .map(Long::parseLong)
                .collect(Collectors.toList());

            PlayerIdListDto playerIdListDto = PlayerIdListDto.builder()
                .playerIdList(playerIdList)
                .build();

            rankCommandService.updateRank("무한 초성 게임", playerIdListDto);

        } catch (Exception e) {
            e.printStackTrace();
        }

        // 4. 세션 정리 전 복사본 만들기
        List<WebSocketSession> sessionsToClean = new ArrayList<>(
            gameRoomSessions.getOrDefault(roomId, new ArrayList<>())
        );

        for (WebSocketSession session : sessionsToClean) {
            try {
                String playerId = sessionPlayerIdMap.get(session);
                if (playerId != null) {
                    playerCommandService.updateStatus(Long.valueOf(playerId), PlayerStatus.none);
                    playerNicknames.remove(playerId);
                    sessionPlayerIdMap.remove(session);
                }
                // 5. 열린 세션 닫기 추가
                if (session.isOpen()) {
                    session.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        gameRoomSessions.remove(roomId);
        turnOrder.remove(roomId);
        usedWords.remove(roomId);
        submittedAnswers.remove(roomId);
        turnTimeoutTasks.remove(roomId);
        gameConsonantsMap.remove(roomId);
        isSinglePlayerGame.remove(roomId);
        gameIdsMap.remove(roomId);
        turnInProgress.remove(roomId);
        activePlayersMap.remove(roomId);

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

    private void broadcastToRoom(Long roomId, String message) {
        List<WebSocketSession> sessions = gameRoomSessions.get(roomId);
        if (sessions != null) {
            for (WebSocketSession session : sessions) {
                try {
                    if (session.isOpen()) {  // 세션이 열려있는 경우에만 메시지 전송
                        session.sendMessage(new TextMessage(message));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
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

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long roomId = getRoomIdFromSession(session);
        String playerId = sessionPlayerIdMap.get(session);

        if (playerId != null) {
            playerCommandService.updateStatus(Long.valueOf(playerId), PlayerStatus.none);
        }

        if (roomId != null && playerId != null) {
            handlePlayerExit(session, roomId, playerId);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        try {
            Long roomId = getRoomIdFromSession(session);
            String playerId = sessionPlayerIdMap.get(session);

            if (roomId != null && playerId != null) {
                handlePlayerExit(session, roomId, playerId);
            }

            if (session.isOpen()) {
                session.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handlePlayerExit(WebSocketSession session, Long roomId, String playerId) {
        try {
            // 플레이어 상태 업데이트
            playerCommandService.updateStatus(Long.parseLong(playerId), PlayerStatus.none);

            // 플레이어의 닉네임 가져오기
            String nickname = playerNicknames.get(playerId);

            // 나간 플레이어를 활성 플레이어 목록에서 제거
            Set<String> activePlayers = activePlayersMap.get(roomId);
            if (activePlayers != null) {
                activePlayers.remove(playerId);
            }

            // 세션 관리 먼저 수행
            List<WebSocketSession> sessions = gameRoomSessions.get(roomId);
            if (sessions != null) {
                sessions.remove(session);
            }

            Room room = roomQueryService.findById(roomId)
                .orElseThrow(() -> new NoRoomException("방을 찾을 수 없습니다."));
            Long creatorId = room.getCreator();

            // 플레이어 수 업데이트
            long playerCount = sessions != null ? sessions.size() : 0;
            roomCommandService.updatePlayerCount(roomId, playerCount);

            // 방장이 나간 경우 방장 할당 및 알림
            if (creatorId.equals(Long.parseLong(playerId))) {
                // 새로운 방장 할당
                if (sessions != null && !sessions.isEmpty()) {
                    WebSocketSession newCreatorSession = sessions.get(0);
                    String newCreatorId = sessionPlayerIdMap.get(newCreatorSession);
                    if (newCreatorId != null) {
                        roomCommandService.updateRoomCreator(roomId, Long.parseLong(newCreatorId));

                        String newCreatorNickname = playerQueryService.findPlayerById(Long.parseLong(newCreatorId)).nickname();
                        String newCreatorMessage = String.format(
                            "{\"type\": \"creator_change\", \"message\": \"%s님이 새로운 방장이 되었습니다.\", \"creatorId\": \"%s\", \"playerCount\": %d}",
                            newCreatorNickname, newCreatorId, playerCount
                        );
                        broadcastToRoom(roomId, newCreatorMessage);
                    }
                }
            }

            // 턴 순서 관련 처리
            Queue<WebSocketSession> roomTurnOrder = turnOrder.get(roomId);
            if (roomTurnOrder != null) {
                WebSocketSession currentTurnSession = roomTurnOrder.peek();

                // 현재 턴이 아닌 플레이어가 나간 경우에만 턴 순서에서 즉시 제거
                if (currentTurnSession != session) {
                    roomTurnOrder.remove(session);
                }
                // 현재 턴인 플레이어는 타임아웃될 때까지 턴 순서에 유지

                boolean shouldEndGame = Boolean.TRUE.equals(isSinglePlayerGame.get(roomId))
                    ? roomTurnOrder.isEmpty()
                    : roomTurnOrder.size() <= 1;

                if (shouldEndGame) {
                    endGame(roomId);
                    return;
                }
            }

            // 나간 플레이어 알림 브로드캐스트
            String exitMessage = String.format(
                "{\"type\": \"exit\", \"playerId\": \"%s\", \"message\": \"%s님이 게임을 나갔습니다.\"}",
                playerId,
                nickname
            );
            broadcastToRoom(roomId, exitMessage);

            // 세션-플레이어 ID 매핑 제거
            sessionPlayerIdMap.remove(session);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
