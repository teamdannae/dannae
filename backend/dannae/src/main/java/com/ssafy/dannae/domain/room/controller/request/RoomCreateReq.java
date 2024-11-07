package com.ssafy.dannae.domain.room.controller.request;

import lombok.Builder;

@Builder
public record RoomCreateReq(
        String title,
        String mode,
        Boolean isPublic
) {
}
