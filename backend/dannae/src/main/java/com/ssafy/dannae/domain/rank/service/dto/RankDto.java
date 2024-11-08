package com.ssafy.dannae.domain.rank.service.dto;

import com.ssafy.dannae.domain.player.entity.PlayerStatus;
import lombok.Builder;

@Builder
public record RankDto(
        Long id,
        Long score,
        String mode,
        String nickname,
        int image
) {

}
