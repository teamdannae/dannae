package com.ssafy.dannae.domain.player.service.dto;

import lombok.Builder;

@Builder
public record PlayerDto(
        Long playerId,
        Long roomId,
        Long score,
        String status,
        String authorization,
        String nickname,
        int image
) {
}
