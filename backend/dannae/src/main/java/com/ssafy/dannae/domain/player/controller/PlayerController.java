package com.ssafy.dannae.domain.player.controller;

import com.ssafy.dannae.domain.room.exception.NoRoomException;
import com.ssafy.dannae.domain.room.service.RoomQueryService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ssafy.dannae.domain.player.controller.request.PlayerReq;
import com.ssafy.dannae.domain.player.controller.response.PlayerRes;
import com.ssafy.dannae.domain.player.entity.PlayerStatus;
import com.ssafy.dannae.domain.player.exception.TokenException;
import com.ssafy.dannae.domain.player.service.PlayerCommandService;
import com.ssafy.dannae.domain.player.service.PlayerQueryService;
import com.ssafy.dannae.domain.player.service.dto.PlayerDto;
import com.ssafy.dannae.global.exception.handler.WaitingRoomWebSocketHandler;
import com.ssafy.dannae.global.template.response.BaseResponse;
import com.ssafy.dannae.global.util.JwtTokenProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/players")
@RestController
public class PlayerController {

    private final JwtTokenProvider jwtTokenProvider;
    private final WaitingRoomWebSocketHandler waitingRoomWebSocketHandler;
    private final PlayerCommandService playerCommandService;
    private final PlayerQueryService playerQueryService;
    private final RoomQueryService roomQueryService;

    @PatchMapping("/ready/{room-id}")
    public ResponseEntity<BaseResponse<?>> updateStatusReady(@RequestHeader(HttpHeaders.AUTHORIZATION) String token, @PathVariable("room-id") Long roomId ) {
        String jwtToken = token.startsWith("Bearer ") ? token.substring(7) : token;

        if (!jwtTokenProvider.validateToken(jwtToken)) {
            throw new TokenException("유효하지 않거나 만료된 토큰입니다.");
        }

        if (roomId == null || !roomQueryService.existsById(roomId)) {
            throw new NoRoomException("Room not found");
        }

        String id = jwtTokenProvider.getPlayerIdFromToken(jwtToken);
        Long playerId = Long.parseLong(id);
        playerCommandService.updateStatus(playerId, PlayerStatus.ready);

        waitingRoomWebSocketHandler.broadcastPlayerStatusUpdate(roomId, playerId, PlayerStatus.ready);
        return ResponseEntity.ok(BaseResponse.ofSuccess());
    }

    @PatchMapping("/nonready/{room-id}")
    public ResponseEntity<BaseResponse<?>> updateStatusNonReady(@RequestHeader(HttpHeaders.AUTHORIZATION) String token , @PathVariable("room-id") Long roomId) {
        String jwtToken = token.startsWith("Bearer ") ? token.substring(7) : token;

        if (!jwtTokenProvider.validateToken(jwtToken)) {
            throw new TokenException("유효하지 않거나 만료된 토큰입니다.");
        }

        if (roomId == null || !roomQueryService.existsById(roomId)) {
            throw new NoRoomException("Room not found");
        }

        String id = jwtTokenProvider.getPlayerIdFromToken(jwtToken);
        Long playerId = Long.parseLong(id);
        playerCommandService.updateStatus(playerId, PlayerStatus.nonready);

        waitingRoomWebSocketHandler.broadcastPlayerStatusUpdate(roomId, playerId, PlayerStatus.nonready);
        return ResponseEntity.ok(BaseResponse.ofSuccess());
    }

    @PostMapping("")
    public ResponseEntity<BaseResponse<PlayerRes>> createPlayer(@RequestBody PlayerReq req){

        PlayerDto playerDto = playerQueryService.createPlayer(PlayerDto.builder()
                .score(0L)
                .status(PlayerStatus.nonready)
                .nickname(req.nickname())
                .image(req.image())
                .build());

        String token = jwtTokenProvider.createToken( playerDto.playerId().toString());

        PlayerRes playerRes = PlayerRes.builder()
                .playerId(playerDto.playerId())
                .token(token)
                .build();

        return ResponseEntity.ok(BaseResponse.ofSuccess(playerRes));
    }
}
