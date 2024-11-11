package com.ssafy.dannae.global.exception.handler;

import com.ssafy.dannae.domain.game.sentencegame.controller.request.SentenceGameReq;
import com.ssafy.dannae.domain.game.sentencegame.controller.response.SentenceGameRes;
import com.ssafy.dannae.domain.game.sentencegame.service.SentenceGameCommandService;
import com.ssafy.dannae.domain.game.sentencegame.service.dto.SentenceGameDto;
import com.ssafy.dannae.domain.player.entity.PlayerStatus;
import com.ssafy.dannae.domain.player.service.PlayerCommandService;
import com.ssafy.dannae.domain.player.service.PlayerQueryService;
import com.ssafy.dannae.domain.player.service.dto.PlayerDto;
import com.ssafy.dannae.global.util.JwtTokenProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Component
public class SentenceGameWebSocketHandler extends TextWebSocketHandler {

    private final Map<Long, List<WebSocketSession>> gameRoomSessions = new ConcurrentHashMap<>();
    private final WaitingRoomWebSocketHandler waitingRoomHandler;
    private final JwtTokenProvider jwtTokenProvider;
    private final PlayerCommandService playerCommandService;
    private final PlayerQueryService playerQueryService;
    private final SentenceGameCommandService sentenceGameCommandService;

    private final int roundTimeLimit = 20;
    private final int roundWaitTime = 5;
    private final Map<Long, Map<String, Boolean>> roundPlayerStatus = new ConcurrentHashMap<>();
    private final Map<WebSocketSession, String> sessionTokenMap = new ConcurrentHashMap<>();
    private int currentRound = 0;
    private final Map<Long, Map<String, String>> playerMessages = new ConcurrentHashMap<>();  // 플레이어 메시지 저장

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

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

        playerCommandService.updateStatus(Long.parseLong(playerId), PlayerStatus.playing); // 관련 메시지 생략...

        session.sendMessage(new TextMessage("{\"type\": \"enter\", \"event\": \"join_game\", \"message\": \"" + nickname + "님이 게임에 연결되었습니다.\", \"playerId\": \"" + playerId + "\", \"nickname\": \"" + nickname + "\", \"status\": \"playing\"}"));

        // 5초 후 게임 시작
        scheduler.schedule(() -> {
            try {
                broadcastToRoom(roomId, "{\"type\": \"game_start\", \"message\": \"게임이 5초 후에 시작됩니다.\"}");
                Thread.sleep(roundWaitTime * 1000); // 5초 대기
                startNewRound(roomId); // 첫 라운드 시작
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, 0, TimeUnit.SECONDS);
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

        if (playerStatus != null && playerStatus.get(playerId)) {
            session.sendMessage(new TextMessage("{\"type\": \"error\", \"message\": \"이미 메시지를 보냈습니다.\"}"));
            return;
        }

        playerStatus.put(playerId, true);
        playerMessages.get(roomId).put(playerId, payload);  // 메시지 저장

        String chatMessage = String.format("{\"type\": \"chat\", \"event\": \"message\", \"playerId\": \"%s\", \"message\": \"%s\"}", playerId, payload);
        session.sendMessage(new TextMessage(chatMessage));  // 플레이어에게 메시지 전송

        if (checkIfAllPlayersSentMessages(roomId)) {
            endRound(roomId); // 모든 플레이어가 메시지를 보냈을 때 라운드 종료
        }
    }

    private void endRound(Long roomId) {
        Map<String, Boolean> playerStatus = roundPlayerStatus.get(roomId);
        Map<String, String> messages = playerMessages.get(roomId);
        List<Long> playerIds = new ArrayList<>();
        List<String> sentences = new ArrayList<>();

        // 플레이어 ID 정렬하여 순서 고정
        List<String> sortedPlayerIds = new ArrayList<>(playerStatus.keySet());
        Collections.sort(sortedPlayerIds);  // 정렬하여 순서 고정

        for (String playerId : sortedPlayerIds) {
            playerIds.add(Long.parseLong(playerId));
            // 플레이어가 메시지를 보냈다면 해당 메시지를 추가하고, 그렇지 않다면 빈 문자열을 추가
            sentences.add(messages.getOrDefault(playerId, ""));
        }

        // SentenceGameReq 생성 및 채점 요청
        SentenceGameReq sentenceGameReq = new SentenceGameReq(roomId, playerIds, sentences);
        SentenceGameRes res = sentenceGameCommandService.playGame(sentenceGameReq);

        // 채점 결과 JSON으로 변환하여 전송
        String scoreMessage = String.format(
                "{\"type\": \"round_end\", \"message\": \"라운드가 종료되었습니다.\", " +
                        "\"isEnd\": %s, \"playerSentences\": %s, \"playerNowScores\": %s, " +
                        "\"playerTotalScores\": %s, \"playerCorrects\": %s, " +
                        "\"activeWords\": %s, \"inactiveWords\": %s}",
                res.isEnd(),
                res.playerSentences(),
                res.playerNowScores(),
                res.playerTotalScores(),
                res.playerCorrects(),
                res.activeWords(),
                res.inactiveWords()
        );

        broadcastToRoom(roomId, scoreMessage);

        // 다음 라운드 또는 게임 종료 처리
        if (res.isEnd()) {
            broadcastToRoom(roomId, "{\"type\": \"game_end\", \"message\": \"게임이 종료되었습니다.\"}");
        } else {
            scheduler.schedule(() -> startNewRound(roomId), roundWaitTime, TimeUnit.SECONDS);
        }
    }

    public void startNewRound(Long roomId) {
        // 라운드 시작 시 currentRound 증가
        currentRound++;

        Map<String, Boolean> playerStatus = new ConcurrentHashMap<>();
        List<WebSocketSession> sessions = gameRoomSessions.get(roomId);

        if (sessions != null) {
            sessions.forEach(session -> {
                String playerId = getPlayerIdFromSession(session);
                playerStatus.put(playerId, false); // 각 플레이어의 메시지 전송 상태 초기화
            });
        }

        roundPlayerStatus.put(roomId, playerStatus);

        SentenceGameDto gameWithWords = null;
        if (currentRound == 1) {
            // 1라운드일 경우에만 단어셋 가져오기
            SentenceGameDto sentenceGameDto = SentenceGameDto.builder()
                    .roomId(roomId)
                    .build();
            gameWithWords = sentenceGameCommandService.createInitial(sentenceGameDto);

            // 1라운드 시작 메시지와 단어셋 브로드캐스트
            broadcastToRoom(roomId, "{\"type\": \"round_start\", \"round\": \"" + currentRound + "\", " +
                    "\"message\": \"" + currentRound + "라운드가 시작되었습니다!\", " +
                    "\"words\": " + gameWithWords.activeWords() + "}");
        } else {
            // 이후 라운드 시작 메시지만 브로드캐스트
            broadcastToRoom(roomId, "{\"type\": \"round_start\", \"round\": \"" + currentRound + "\", " +
                    "\"message\": \"" + currentRound + "라운드가 시작되었습니다!\"}");
        }

        // 타이머 시작
        scheduler.schedule(() -> {
            if (!checkIfAllPlayersSentMessages(roomId)) {
                endRound(roomId); // 시간 초과로 라운드 종료
            }
        }, roundTimeLimit, TimeUnit.SECONDS);
    }

    private String getPlayerIdFromSession(WebSocketSession session) {
        String token = sessionTokenMap.get(session);
        return token != null ? jwtTokenProvider.getPlayerIdFromToken(token) : null;
    }

    // 모든 플레이어가 메시지를 보냈는지 확인
    private boolean checkIfAllPlayersSentMessages(Long roomId) {
        Map<String, Boolean> playerStatus = roundPlayerStatus.get(roomId);
        return playerStatus != null && playerStatus.values().stream().allMatch(sent -> sent);
    }

    private void broadcastToRoom(Long roomId, String message) {
        List<WebSocketSession> sessions = gameRoomSessions.get(roomId);
        if (sessions != null) {
            for (WebSocketSession session : sessions) {
                try {
                    session.sendMessage(new TextMessage(message)); // 메시지를 각 세션에 전송
                } catch (IOException e) {
                    System.err.println("Failed to send message to session " + session.getId() + ": " + e.getMessage());
                }
            }
        } else {
            System.out.println("No active sessions found for roomId " + roomId); // 방이 존재하지 않는 경우 로그 출력
        }
    }

}