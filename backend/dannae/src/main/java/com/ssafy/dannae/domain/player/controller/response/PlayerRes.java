package com.ssafy.dannae.domain.player.controller.response;

import lombok.Builder;

@Builder
public record PlayerRes (
        Long playerId,
        String token
){
}
