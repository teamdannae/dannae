package com.ssafy.dannae.domain.player.controller.request;

import lombok.Builder;

@Builder
public record PlayerReq(
        String nickname,
        int image
) {
}

