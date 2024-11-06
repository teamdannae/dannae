package com.ssafy.dannae.domain.player.controller;

import com.ssafy.dannae.domain.player.controller.response.PlayerRes;
import com.ssafy.dannae.domain.player.entity.PlayerAuthorization;
import com.ssafy.dannae.domain.player.entity.PlayerStatus;
import com.ssafy.dannae.domain.player.exception.TokenException;
import com.ssafy.dannae.domain.player.service.PlayerCommandService;
import com.ssafy.dannae.domain.player.service.PlayerQueryService;
import com.ssafy.dannae.domain.player.service.dto.PlayerDto;
import com.ssafy.dannae.domain.room.controller.request.RoomCreaterReq;
import com.ssafy.dannae.domain.room.service.dto.RoomDto;
import com.ssafy.dannae.global.exception.ResponseCode;
import com.ssafy.dannae.global.exception.handler.WaitingRoomWebSocketHandler;
import com.ssafy.dannae.global.template.response.BaseResponse;
import com.ssafy.dannae.global.util.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/players")
@RestController
public class PlayerController {

    private final JwtTokenProvider jwtTokenProvider;
    private final WaitingRoomWebSocketHandler waitingRoomWebSocketHandler;
    private final PlayerCommandService playerCommandService;
    private final PlayerQueryService playerQueryService;

    @PatchMapping("/ready")
    public ResponseEntity<BaseResponse<?>> updateStatusReady(@RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        String jwtToken = token.startsWith("Bearer ") ? token.substring(7) : token;

        if (!jwtTokenProvider.validateToken(jwtToken)) {
            throw new TokenException("유효하지 않거나 만료된 토큰입니다.");
        }

        String id = jwtTokenProvider.getPlayerIdFromToken(jwtToken);
        Long playerId = Long.parseLong(id);
        playerCommandService.updateStatus(playerId, PlayerStatus.ready);

        waitingRoomWebSocketHandler.broadcastPlayerStatusUpdate(playerId, PlayerStatus.ready);
        return ResponseEntity.ok(BaseResponse.ofSuccess());
    }

    @PostMapping("")
    public ResponseEntity<BaseResponse<PlayerRes>> createPlayer(@RequestBody RoomCreaterReq req){

        PlayerDto playerDto = playerQueryService.createPlayer(PlayerDto.builder()
                .score(0L)
                .status(PlayerStatus.nonready)
                .authorization(PlayerAuthorization.creator)
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
