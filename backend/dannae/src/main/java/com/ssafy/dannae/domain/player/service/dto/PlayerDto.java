package com.ssafy.dannae.domain.player.service.dto;

import com.ssafy.dannae.domain.player.entity.PlayerStatus;
import lombok.Builder;

@Builder
public record PlayerDto(
        Long playerId,
        Long score,
        PlayerStatus status,
        String nickname,
        int image
) {
}
