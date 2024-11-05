package com.ssafy.dannae.domain.player.service.dto;

import com.ssafy.dannae.domain.player.entity.PlayerAuthorization;
import com.ssafy.dannae.domain.player.entity.PlayerStatus;
import lombok.Builder;

@Builder
public record PlayerDto(
        Long playerId,
        Long roomId,
        Long score,
        PlayerStatus status,
        PlayerAuthorization authorization,
        String nickname,
        int image
) {
}
