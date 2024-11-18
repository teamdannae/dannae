package com.ssafy.dannae.global.exception.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.ssafy.dannae.domain.game.sentencegame.controller.request.SentenceGameReq;
import com.ssafy.dannae.domain.game.sentencegame.controller.response.SentenceGameCreateRes;
import com.ssafy.dannae.domain.game.sentencegame.controller.response.SentenceGameRes;
import com.ssafy.dannae.domain.game.sentencegame.service.SentenceGameCommandService;
import com.ssafy.dannae.domain.game.sentencegame.service.dto.SentenceGameDto;
import com.ssafy.dannae.domain.game.sentencegame.service.dto.SentencePlayerDto;
import com.ssafy.dannae.domain.game.sentencegame.service.dto.SentenceWordDto;
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

import jakarta.annotation.PreDestroy;

@Component
public class SentenceGameWebSocketHandler extends TextWebSocketHandler {

    private final Map<Long, List<WebSocketSession>> gameRoomSessions = new ConcurrentHashMap<>();
    private final Map<Long, ScheduledExecutorService> roomSchedulers = new ConcurrentHashMap<>(); // 각 방별로 스케줄러 관리
    private final JwtTokenProvider jwtTokenProvider;
    private final PlayerCommandService playerCommandService;
    private final RoomCommandService roomCommandService;
    private final PlayerQueryService playerQueryService;
    private final SentenceGameCommandService sentenceGameCommandService;
    private final RoomQueryService roomQueryService;
    private final RankCommandService rankCommandService;
    private final int roundTimeLimit = 20;
    private final int roundWaitTime = 5;
    private final Map<Long, Map<String, Boolean>> roundPlayerStatus = new ConcurrentHashMap<>();
    private final Map<WebSocketSession, String> sessionTokenMap = new ConcurrentHashMap<>();
    private final Map<Long, Integer> currentRoundMap = new ConcurrentHashMap<>();  // 방마다 라운드를 관리
    private final Map<Long, Map<String, String>> playerMessages = new ConcurrentHashMap<>();  // 플레이어 메시지 저장
    private final Map<Long, AtomicBoolean> isRoundInProgressMap = new ConcurrentHashMap<>();
    private final Map<Long, AtomicBoolean> isRoundEndInProgressMap = new ConcurrentHashMap<>(); // 라운드 종료 진행 여부 플래그
    private final Map<Long, ScheduledFuture<?>> roundTimeoutTasks = new ConcurrentHashMap<>();
    private final Map<Long, AtomicBoolean> gameStartedMap = new ConcurrentHashMap<>(); // 방별 게임 시작 플래그
    private final Map<Long, AtomicBoolean> isGameEndInProgressMap = new ConcurrentHashMap<>();


    public SentenceGameWebSocketHandler(JwtTokenProvider jwtTokenProvider, PlayerCommandService playerCommandService, PlayerQueryService playerQueryService, SentenceGameCommandService sentenceGameCommandService, RoomQueryService roomQueryService,  RoomCommandService roomCommandService,
                                        RankCommandService rankCommandService) {
        this.roomQueryService = roomQueryService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.playerCommandService = playerCommandService;
        this.playerQueryService = playerQueryService;
        this.sentenceGameCommandService = sentenceGameCommandService;
        this.roomCommandService = roomCommandService;
        this.rankCommandService = rankCommandService;
    }

    private final ScheduledExecutorService globalScheduler = Executors.newScheduledThreadPool(
            1,
            r -> {
                Thread thread = new Thread(r);
                thread.setDaemon(false); // 데몬 쓰레드가 아닌 일반 쓰레드로 설정
                return thread;
            }
    );

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long roomId = getRoomIdFromSession(session);
        String token = getTokenFromSession(session);
        String playerId = jwtTokenProvider.getPlayerIdFromToken(token);
        PlayerDto dto = playerQueryService.findPlayerById(Long.parseLong(playerId));
        String nickname = dto.nickname();

        if (!jwtTokenProvider.validateToken(token)) {
            session.sendMessage(new TextMessage("{\"type\": \"error\", \"event\": \"invalid_token\", \"message\": \"잘못된 토큰이어서, 입장한 사용자만 게임에 참여할 수 있습니다.\"}"));
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        List<WebSocketSession> sessions = gameRoomSessions.computeIfAbsent(roomId, k -> new CopyOnWriteArrayList<>());
        sessions.add(session);
        sessionTokenMap.put(session, token);

        playerCommandService.updateStatus(Long.parseLong(playerId), PlayerStatus.playing);

        session.sendMessage(new TextMessage("{\"type\": \"enter\", \"event\": \"join_game\", \"message\": \"" + nickname + "님이 게임에 연결되었습니다.\", \"playerId\": \"" + playerId + "\", \"nickname\": \"" + nickname + "\", \"status\": \"playing\"}"));
        currentRoundMap.put(roomId, 0);

        roomSchedulers.computeIfAbsent(roomId, k -> Executors.newScheduledThreadPool(1));
        gameStartedMap.putIfAbsent(roomId, new AtomicBoolean(false)); // 초기화

        // 모든 플레이어가 입장한 경우, 게임이 시작되지 않았다면 시작
        if (areAllPlayersInGameRoom(roomId) && gameStartedMap.get(roomId).compareAndSet(false, true)) {
            startGame(roomId);
        }
    }

    private void startGame(Long roomId) {
        try {
            broadcastToRoom(roomId, "{\"type\": \"game_start\", \"message\": \"5초 후에 게임이 시작됩니다.\"}");

            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(roundWaitTime * 1000);
                    startNewRound(roomId);
                } catch (InterruptedException e) {
                    System.err.println("Game start delayed task interrupted: " + e.getMessage());
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    System.err.println("Error during game start: " + e.getMessage());
                    e.printStackTrace();
                    broadcastToRoom(roomId, "{\"type\": \"error\", \"message\": \"게임 시작 중 오류가 발생했습니다.\"}");
                }
            }, globalScheduler);

        } catch (Exception e) {
            System.err.println("Failed to start game for room " + roomId + ": " + e.getMessage());
            e.printStackTrace();
            broadcastToRoom(roomId, "{\"type\": \"error\", \"message\": \"게임을 시작할 수 없습니다.\"}");
        }
    }

    @PreDestroy
    public void destroy() {
        try {
            for (Map.Entry<Long, ScheduledExecutorService> entry : roomSchedulers.entrySet()) {
                ScheduledExecutorService scheduler = entry.getValue();
                scheduler.shutdownNow();
            }

            globalScheduler.shutdown();
            if (!globalScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                globalScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    public void startNewRound(Long roomId) {
        isRoundInProgressMap.putIfAbsent(roomId, new AtomicBoolean(false));

        if (!isRoundInProgressMap.get(roomId).compareAndSet(false, true)) {
            System.out.println("방 " + roomId + "에서 이미 라운드가 진행 중입니다.");
            return;
        }

        try {
            if (roomId == null) {
                System.err.println("Error: roomId is null");
                broadcastToRoom(roomId, "{\"type\": \"error\", \"message\": \"방 ID가 유효하지 않습니다.\"}");
                return;
            }

            List<WebSocketSession> sessions = gameRoomSessions.get(roomId);
            if (sessions == null || sessions.isEmpty()) {
                System.err.println("Error: No active sessions found for room " + roomId);
                broadcastToRoom(roomId, "{\"type\": \"error\", \"message\": \"활성화된 세션이 없습니다.\"}");
                return;
            }

            int currentRound = currentRoundMap.getOrDefault(roomId, 0) + 1;
            currentRoundMap.put(roomId, currentRound);

            Map<String, Boolean> playerStatus = new ConcurrentHashMap<>();
            sessions.forEach(session -> {
                String playerId = getPlayerIdFromSession(session);
                if (playerId != null) {
                    playerStatus.put(playerId, false);
                }
            });
            roundPlayerStatus.put(roomId, playerStatus);

            playerMessages.put(roomId, new ConcurrentHashMap<>());

            if (currentRound == 1) {
                initializeFirstRound(roomId, currentRound);
            } else {
                String roundStartMessage = String.format(
                        "{\"type\": \"round_start\", \"round\": \"%d\", \"message\": \"%d라운드가 시작되었습니다!\"}",
                        currentRound,
                        currentRound
                );
                broadcastToRoom(roomId, roundStartMessage);
            }

            ScheduledFuture<?> previousTask = roundTimeoutTasks.get(roomId);
            if (previousTask != null && !previousTask.isDone()) {
                previousTask.cancel(true);
            }

            ScheduledFuture<?> timeoutTask = roomSchedulers.get(roomId).schedule(() -> {
                if (!checkIfAllPlayersSentMessages(roomId)) {
                    endRound(roomId);
                }
            }, roundTimeLimit, TimeUnit.SECONDS);
            roundTimeoutTasks.put(roomId, timeoutTask);

        } catch (Exception e) {
            System.err.println("Critical error in startNewRound for room " + roomId);
            e.printStackTrace();
            String errorDetail = e.getMessage() != null ? e.getMessage() : "알 수 없는 오류";
            broadcastToRoom(roomId, "{\"type\": \"error\", \"message\": \"새 라운드 시작 오류: " + errorDetail + "\"}");
        } finally {
            isRoundInProgressMap.get(roomId).set(false);
        }
    }

    private void initializeFirstRound(Long roomId, int currentRound) {
        try {
            SentenceGameDto sentenceGameDto = SentenceGameDto.builder()
                    .roomId(roomId)
                    .build();

            SentenceGameCreateRes gameWithWords = sentenceGameCommandService.createInitial(sentenceGameDto);

            List<SentenceWordDto> words = gameWithWords.words();
            StringBuilder wordsJson = new StringBuilder("[");
            for (SentenceWordDto word : words) {
                wordsJson.append(String.format(
                        "{\"word\": \"%s\", \"difficulty\": %d},",
                        word.word(),
                        word.difficulty()
                ));
            }
            if (wordsJson.charAt(wordsJson.length() - 1) == ',') {
                wordsJson.deleteCharAt(wordsJson.length() - 1);
            }
            wordsJson.append("]");

            String roundStartMessage = String.format(
                    "{\"type\": \"round_start\", \"round\": \"%d\", \"message\": \"%d라운드가 시작되었습니다!\", \"words\": %s}",
                    currentRound,
                    currentRound,
                    wordsJson.toString()
            );
            broadcastToRoom(roomId, roundStartMessage);

        } catch (Exception e) {
            System.err.println("Error during first round initialization: " + e.getMessage());
            e.printStackTrace();
            broadcastToRoom(roomId, "{\"type\": \"error\", \"message\": \"첫 라운드 초기화 중 오류: " + e.getMessage() + "\"}");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws IOException {
        Long roomId = getRoomIdFromSession(session);
        String playerId = jwtTokenProvider.getPlayerIdFromToken(getTokenFromSession(session));
        PlayerDto dto = playerQueryService.findPlayerById(Long.parseLong(playerId));
        String nickname = dto.nickname();
        List<WebSocketSession> sessions = gameRoomSessions.get(roomId);
        playerCommandService.updateStatus(Long.valueOf(playerId),PlayerStatus.none);
        sessionTokenMap.remove(session);

        if (sessions != null) {
            sessions.remove(session);

            // 방이 비었으면 종료
            if (sessions.isEmpty()) {
                gameRoomSessions.remove(roomId);
                ScheduledExecutorService scheduler = roomSchedulers.remove(roomId);
                if (scheduler != null) scheduler.shutdownNow();
                return;
            }

            // 다른 사용자에게 플레이어 나감 메시지 전송
            broadcastToRoom(roomId, String.format("{\"type\": \"leave\", \"event\": \"disconnect\", \"message\": \"%s님이 게임을 나갔습니다.\", \"playerId\": \"%s\", \"nickname\": \"%s\"}", nickname, playerId, nickname));

            // 방장의 playerId 가져오기
            Room room = roomQueryService.findById(roomId)
                    .orElseThrow(() -> new NoRoomException("방을 찾을 수 없습니다."));
            Long creatorId = room.getCreator();

            // 플레이어 수 업데이트
            int playerCount = getRoomPlayerCount(roomId);
            roomCommandService.updatePlayerCount(roomId, (long) playerCount);

            // 방장이 나간 경우 방장 할당
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
                roomCommandService.updateRoomCreator(roomId, newCreatorId);
            }
        } catch (Exception e) {
            System.err.println("Error in assignNewCreator: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public int getRoomPlayerCount(Long roomId) {
        List<WebSocketSession> sessions = gameRoomSessions.get(roomId);
        return sessions != null ? sessions.size() : 0;
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

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        Long roomId = getRoomIdFromSession(session);
        String playerId = jwtTokenProvider.getPlayerIdFromToken(getTokenFromSession(session));
        String payload = message.getPayload();
        Map<String, Boolean> playerStatus = roundPlayerStatus.get(roomId);
        playerMessages.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>());
        String nickname = playerQueryService.findPlayerById(Long.parseLong(playerId)).nickname();

        JSONObject jsonMessage = new JSONObject(payload);
        String messageContent = jsonMessage.getString("message");

        if (playerStatus != null && playerStatus.get(playerId)) {
            session.sendMessage(new TextMessage("{\"type\": \"error\", \"message\": \"이미 메시지를 보냈습니다.\"}"));
            return;
        }

        playerStatus.put(playerId, true);
        playerMessages.get(roomId).put(playerId, messageContent);

        JSONObject chatMessage = new JSONObject();
        chatMessage.put("type", "chat");
        chatMessage.put("event", "message");
        chatMessage.put("playerId", playerId);
        chatMessage.put("nickname", nickname);
        chatMessage.put("message", messageContent);

        session.sendMessage(new TextMessage(chatMessage.toString()));

        if (checkIfAllPlayersSentMessages(roomId)) {
            endRound(roomId);
        }
    }

    private void endRound(Long roomId) {
        isRoundEndInProgressMap.putIfAbsent(roomId, new AtomicBoolean(false));
        if (!isRoundEndInProgressMap.get(roomId).compareAndSet(false, true)) {
            System.out.println("방 " + roomId + "에서 이미 라운드가 종료 진행 중입니다.");
            return;
        }

        try {
            Map<String, Boolean> playerStatus = roundPlayerStatus.get(roomId);
            Map<String, String> messages = playerMessages.get(roomId);
            List<Long> playerIds = new ArrayList<>();
            List<String> sentences = new ArrayList<>();

            List<String> sortedPlayerIds = new ArrayList<>(playerStatus.keySet());
            Collections.sort(sortedPlayerIds);

            for (String playerId : sortedPlayerIds) {
                playerIds.add(Long.parseLong(playerId));
                sentences.add(messages.getOrDefault(playerId, ""));
            }

            SentenceGameReq sentenceGameReq = new SentenceGameReq(roomId, playerIds, sentences);
            SentenceGameRes res = sentenceGameCommandService.playGame(sentenceGameReq);

            StringBuilder playersJson = new StringBuilder("[");
            for (SentencePlayerDto playerDto : res.playerDtos()) {
                PlayerDto playerDetails = playerQueryService.findPlayerById(playerDto.playerId());
                playersJson.append(String.format(
                        "{\"playerId\": %d, \"nickname\": \"%s\", \"playerCorrects\": %d, \"playerNowScore\": %d, \"playerTotalScore\": %d, \"playerSentence\": \"%s\"},",
                        playerDto.playerId(),
                        playerDetails.nickname(),
                        playerDto.playerCorrects(),
                        playerDto.playerNowScore(),
                        playerDto.playerTotalScore(),
                        playerDto.playerSentence()
                ));
            }
            if (playersJson.charAt(playersJson.length() - 1) == ',') {
                playersJson.deleteCharAt(playersJson.length() - 1);
            }
            playersJson.append("]");
            JSONArray userWordsJson = new JSONArray(res.userWords());
            String scoreMessage = String.format(
                    "{\"type\": \"round_end\", \"message\": \"라운드가 종료되었습니다.\", " +
                            "\"isEnd\": %s, \"userWords\": %s, \"playerDtos\": %s}",
                    res.isEnd(),
                    userWordsJson.toString(),
                    playersJson
            );

            broadcastToRoom(roomId, scoreMessage);

            if (res.isEnd()) {
                List<WebSocketSession> sessions = gameRoomSessions.get(roomId);
                if (sessions != null) {
                    for (WebSocketSession session : sessions) {
                        String playerId = getPlayerIdFromSession(session);
                        if (playerId != null) {
                            playerCommandService.updateStatus(Long.parseLong(playerId), PlayerStatus.none);
                        }
                    }
                }

                broadcastToRoom(roomId, "{\"type\": \"game_end\", \"message\": \"게임이 종료되었습니다.\"}");
                roomCommandService.updateStatus(roomId);

                // 게임 종료 및 랭크 업데이트를 별도의 동기화 블록에서 실행
                endGameAndUpdateRank(roomId);
            } else {
                ScheduledExecutorService scheduler = roomSchedulers.get(roomId);
                scheduler.schedule(() -> startNewRound(roomId), roundWaitTime, TimeUnit.SECONDS);
            }

        } catch (Exception e) {
            System.err.println("Error in endRound for room " + roomId);
            e.printStackTrace();
        } finally {
            isRoundEndInProgressMap.get(roomId).set(false);
        }
    }

    private boolean checkIfAllPlayersSentMessages(Long roomId) {
        Map<String, Boolean> playerStatus = roundPlayerStatus.get(roomId);
        return playerStatus != null && playerStatus.values().stream().allMatch(sent -> sent);
    }

    private void broadcastToRoom(Long roomId, String message) {
        List<WebSocketSession> sessions = gameRoomSessions.get(roomId);
        if (sessions != null) {
            for (WebSocketSession session : sessions) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    System.err.println("Failed to send message to session " + session.getId() + ": " + e.getMessage());
                }
            }
        } else {
            System.out.println("No active sessions found for roomId " + roomId);
        }
    }

    private boolean areAllPlayersInGameRoom(Long roomId) {
        Long activePlayerCount = roomQueryService.findById(roomId)
                .orElseThrow(() -> new NoRoomException("no room exception"))
                .getPlayerCount();
        int currentSessionCount = gameRoomSessions.get(roomId).size();
        return currentSessionCount == activePlayerCount;
    }

    private String getPlayerIdFromSession(WebSocketSession session) {
        String token = sessionTokenMap.get(session);
        return token != null ? jwtTokenProvider.getPlayerIdFromToken(token) : null;
    }

    private synchronized void endGameAndUpdateRank(Long roomId) {
        isGameEndInProgressMap.putIfAbsent(roomId, new AtomicBoolean(false));
        if (!isGameEndInProgressMap.get(roomId).compareAndSet(false, true)) {
            System.out.println("랭크 업데이트가 이미 진행 중입니다. 방 ID: " + roomId);
            return;
        }

        try {
            // 현재 방에 있는 실제 게임 세션들만 가져옴
            List<WebSocketSession> activeSessions = gameRoomSessions.get(roomId);
            if (activeSessions == null || activeSessions.isEmpty()) {
                System.out.println("활성 세션이 없습니다. 방 ID: " + roomId);
                return;
            }

            // 현재 활성화된 세션의 플레이어 ID만 수집
            List<Long> playerIdList = activeSessions.stream()
                    .map(this::getPlayerIdFromSession)
                    .filter(playerId -> playerId != null)
                    .map(Long::parseLong)
                    .distinct()
                    .collect(Collectors.toList());

            if (playerIdList.isEmpty()) {
                System.out.println("유효한 플레이어가 없습니다. 방 ID: " + roomId);
                return;
            }

            System.out.println("랭크 업데이트 시작. 방 ID: " + roomId + ", 플레이어 수: " + playerIdList.size());

            PlayerIdListDto playerIdListDto = PlayerIdListDto.builder()
                    .playerIdList(playerIdList)
                    .build();

            // 랭크 업데이트 실행
            rankCommandService.updateRank("단어의 방", playerIdListDto);
            System.out.println("랭크 업데이트 완료. 방 ID: " + roomId);

            // 게임 상태 초기화
            resetRoomState(roomId);

        } catch (Exception e) {
            System.err.println("랭크 업데이트 중 오류 발생. 방 ID: " + roomId);
            e.printStackTrace();
        } finally {
            isGameEndInProgressMap.get(roomId).set(false);
        }
    }
    private void resetRoomState(Long roomId) {
        if (!gameRoomSessions.containsKey(roomId)) {
            return; // 이미 초기화된 상태라면 무시
        }
        gameStartedMap.remove(roomId);
        currentRoundMap.remove(roomId);
        roundPlayerStatus.remove(roomId);
        playerMessages.remove(roomId);
        isRoundInProgressMap.remove(roomId);
        isRoundEndInProgressMap.remove(roomId);
        roundTimeoutTasks.remove(roomId);

        ScheduledExecutorService scheduler = roomSchedulers.remove(roomId);
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

}
