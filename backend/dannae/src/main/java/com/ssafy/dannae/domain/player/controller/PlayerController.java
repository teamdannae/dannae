package com.ssafy.dannae.domain.player.controller;

import com.ssafy.dannae.domain.player.entity.PlayerStatus;
import com.ssafy.dannae.domain.player.exception.TokenException;
import com.ssafy.dannae.domain.player.service.PlayerCommandService;
import com.ssafy.dannae.global.exception.ResponseCode;
import com.ssafy.dannae.global.exception.handler.WaitingRoomWebSocketHandler;
import com.ssafy.dannae.global.template.response.BaseResponse;
import com.ssafy.dannae.global.util.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/players")
@RestController
public class PlayerController {

    private final JwtTokenProvider jwtTokenProvider;
    private final WaitingRoomWebSocketHandler waitingRoomWebSocketHandler;
    private final PlayerCommandService playerCommandService;


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

}
