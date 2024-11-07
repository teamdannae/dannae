package com.ssafy.dannae.domain.room.controller.request;

import lombok.Builder;

@Builder
public record RoomCreaterReq(
        String title,
        String mode,
        Boolean release
) {
}
