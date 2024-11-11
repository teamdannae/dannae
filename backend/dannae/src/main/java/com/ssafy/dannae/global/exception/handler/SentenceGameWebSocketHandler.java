package com.ssafy.dannae.global.exception.handler;

import com.ssafy.dannae.domain.game.sentencegame.controller.request.SentenceGameReq;
import com.ssafy.dannae.domain.game.sentencegame.controller.response.SentenceGameRes;
import com.ssafy.dannae.domain.game.sentencegame.service.SentenceGameCommandService;
import com.ssafy.dannae.domain.game.sentencegame.service.dto.SentenceGameDto;
import com.ssafy.dannae.domain.game.sentencegame.service.dto.SentencePlayerDto;
import com.ssafy.dannae.domain.player.entity.PlayerStatus;
import com.ssafy.dannae.domain.player.service.PlayerCommandService;
import com.ssafy.dannae.domain.player.service.PlayerQueryService;
import com.ssafy.dannae.domain.player.service.dto.PlayerDto;
import com.ssafy.dannae.global.util.JwtTokenProvider;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

@Component
public class SentenceGameWebSocketHandler extends TextWebSocketHandler {

    private final Map<Long, List<WebSocketSession>> gameRoomSessions = new ConcurrentHashMap<>();
    private final Map<Long, ScheduledExecutorService> roomSchedulers = new ConcurrentHashMap<>(); // 각 방별로 스케줄러 관리
    private final WaitingRoomWebSocketHandler waitingRoomHandler;
    private final JwtTokenProvider jwtTokenProvider;
    private final PlayerCommandService playerCommandService;
    private final PlayerQueryService playerQueryService;
    private final SentenceGameCommandService sentenceGameCommandService;

    private final int roundTimeLimit = 20;
    private final int roundWaitTime = 5;
    private final Map<Long, Map<String, Boolean>> roundPlayerStatus = new ConcurrentHashMap<>();
    private final Map<WebSocketSession, String> sessionTokenMap = new ConcurrentHashMap<>();
    private int currentRound;
    private final Map<Long, Map<String, String>> playerMessages = new ConcurrentHashMap<>();  // 플레이어 메시지 저장

    public SentenceGameWebSocketHandler(WaitingRoomWebSocketHandler waitingRoomHandler, JwtTokenProvider jwtTokenProvider, PlayerCommandService playerCommandService, PlayerQueryService playerQueryService, SentenceGameCommandService sentenceGameCommandService) {
        this.waitingRoomHandler = waitingRoomHandler;
        this.jwtTokenProvider = jwtTokenProvider;
        this.playerCommandService=playerCommandService;
        this.playerQueryService= playerQueryService;
        this.sentenceGameCommandService = sentenceGameCommandService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long roomId = getRoomIdFromSession(session);
        String token = getTokenFromSession(session);
        String playerId = jwtTokenProvider.getPlayerIdFromToken(token);
        PlayerDto dto = playerQueryService.findPlayerById(Long.parseLong(playerId));
        String nickname = dto.nickname();

        if (!jwtTokenProvider.validateToken(token) || !waitingRoomHandler.isPlayerInWaitingRoom(roomId, playerId)) {
            session.sendMessage(new TextMessage("{\"type\": \"error\", \"event\": \"invalid_token\", \"message\": \"잘못된 토큰이거나 대기실에 입장한 사용자만 게임에 참여할 수 있습니다.\"}"));
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        List<WebSocketSession> sessions = gameRoomSessions.computeIfAbsent(roomId, k -> new CopyOnWriteArrayList<>());
        sessions.add(session);
        sessionTokenMap.put(session, token);

        playerCommandService.updateStatus(Long.parseLong(playerId), PlayerStatus.playing);

        session.sendMessage(new TextMessage("{\"type\": \"enter\", \"event\": \"join_game\", \"message\": \"" + nickname + "님이 게임에 연결되었습니다.\", \"playerId\": \"" + playerId + "\", \"nickname\": \"" + nickname + "\", \"status\": \"playing\"}"));
        currentRound = 0;

        // 방별로 스케줄러를 생성하여 저장
        roomSchedulers.computeIfAbsent(roomId, k -> Executors.newScheduledThreadPool(1));
        if (areAllPlayersInGameRoom(roomId)) {
            startGame(roomId);
        }
    }

    private void startGame(Long roomId) {
        broadcastToRoom(roomId, "{\"type\": \"game_start\", \"message\": \"5초 후에 게임이 시작됩니다.\"}");
        ScheduledExecutorService scheduler = roomSchedulers.get(roomId);
        scheduler.schedule(() -> startNewRound(roomId), roundWaitTime, TimeUnit.SECONDS);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws IOException {
        Long roomId = getRoomIdFromSession(session);
        String playerId = jwtTokenProvider.getPlayerIdFromToken(getTokenFromSession(session));
        PlayerDto dto = playerQueryService.findPlayerById(Long.parseLong(playerId));
        String nickname = dto.nickname();
        List<WebSocketSession> sessions = gameRoomSessions.get(roomId);

        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                gameRoomSessions.remove(roomId);
                ScheduledExecutorService scheduler = roomSchedulers.remove(roomId);
                if (scheduler != null) scheduler.shutdownNow();
            } else {
                broadcastToRoom(roomId, String.format("{\"type\": \"leave\", \"event\": \"disconnect\", \"message\": \"%s님이 게임을 나갔습니다.\", \"playerId\": \"%s\", \"nickname\": \"%s}", nickname, playerId, nickname));
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

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        Long roomId = getRoomIdFromSession(session);
        String playerId = jwtTokenProvider.getPlayerIdFromToken(getTokenFromSession(session));
        String payload = message.getPayload();
        Map<String, Boolean> playerStatus = roundPlayerStatus.get(roomId);
        playerMessages.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>());
        String nickname =playerQueryService.findPlayerById(Long.parseLong(playerId)).nickname();

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
        chatMessage.put("nickname",nickname);
        chatMessage.put("message", messageContent);

        session.sendMessage(new TextMessage(chatMessage.toString()));

        if (checkIfAllPlayersSentMessages(roomId)) {
            endRound(roomId);
        }
    }

    private void endRound(Long roomId) {
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

        for (String playerId : playerStatus.keySet()) {
            if (!playerStatus.get(playerId)) {
                WebSocketSession session = getSessionByPlayerId(roomId, playerId);
                if (session != null && session.isOpen()) {
                    String timeoutMessage = String.format(
                            "{\"type\": \"notification\", \"message\": \"제한 시간 내 문장을 입력하지 못했습니다.\"}"
                    );
                    try {
                        session.sendMessage(new TextMessage(timeoutMessage));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
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

        String scoreMessage = String.format(
                "{\"type\": \"round_end\", \"message\": \"라운드가 종료되었습니다.\", " +
                        "\"isEnd\": %s, \"userWords\": %s, \"players\": %s}",
                res.isEnd(),
                res.userWords(),
                playersJson
        );

        broadcastToRoom(roomId, scoreMessage);

        if (res.isEnd()) {
            broadcastToRoom(roomId, "{\"type\": \"game_end\", \"message\": \"게임이 종료되었습니다.\"}");
        } else {
            ScheduledExecutorService scheduler = roomSchedulers.get(roomId);
            scheduler.schedule(() -> startNewRound(roomId), roundWaitTime, TimeUnit.SECONDS);
        }
    }

    public void startNewRound(Long roomId) {
        currentRound++;

        Map<String, Boolean> playerStatus = new ConcurrentHashMap<>();
        List<WebSocketSession> sessions = gameRoomSessions.get(roomId);

        if (sessions != null) {
            sessions.forEach(session -> {
                String playerId = getPlayerIdFromSession(session);
                playerStatus.put(playerId, false);
            });
        }

        roundPlayerStatus.put(roomId, playerStatus);

        SentenceGameDto gameWithWords = null;
        if (currentRound == 1) {
            SentenceGameDto sentenceGameDto = SentenceGameDto.builder()
                    .roomId(roomId)
                    .build();
            gameWithWords = sentenceGameCommandService.createInitial(sentenceGameDto);

            broadcastToRoom(roomId, "{\"type\": \"round_start\", \"round\": \"" + currentRound + "\", " +
                    "\"message\": \"" + currentRound + "라운드가 시작되었습니다!\", " +
                    "\"words\": " + gameWithWords.activeWords() + "}");
        } else {
            broadcastToRoom(roomId, "{\"type\": \"round_start\", \"round\": \"" + currentRound + "\", " +
                    "\"message\": \"" + currentRound + "라운드가 시작되었습니다!\"}");
        }

        ScheduledExecutorService scheduler = roomSchedulers.get(roomId);
        scheduler.schedule(() -> {
            if (!checkIfAllPlayersSentMessages(roomId)) {
                endRound(roomId);
            }
        }, roundTimeLimit, TimeUnit.SECONDS);
    }

    private String getPlayerIdFromSession(WebSocketSession session) {
        String token = sessionTokenMap.get(session);
        return token != null ? jwtTokenProvider.getPlayerIdFromToken(token) : null;
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
        List<String> waitingRoomPlayerIds = waitingRoomHandler.getWaitingRoomPlayers(roomId);
        List<WebSocketSession> gameRoomSessions = this.gameRoomSessions.get(roomId);

        return gameRoomSessions != null &&
                waitingRoomPlayerIds.size() == gameRoomSessions.size() &&
                gameRoomSessions.stream()
                        .map(this::getPlayerIdFromSession)
                        .allMatch(waitingRoomPlayerIds::contains);
    }

    private WebSocketSession getSessionByPlayerId(Long roomId, String playerId) {
        List<WebSocketSession> sessions = gameRoomSessions.get(roomId);
        if (sessions != null) {
            for (WebSocketSession session : sessions) {
                if (playerId.equals(getPlayerIdFromSession(session))) {
                    return session;
                }
            }
        }
        return null;
    }
}
