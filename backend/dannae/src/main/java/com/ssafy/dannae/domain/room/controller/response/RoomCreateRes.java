package com.ssafy.dannae.domain.room.controller.response;

import lombok.Builder;

@Builder
public record RoomCreateRes(
	Long roomId
) {
}
