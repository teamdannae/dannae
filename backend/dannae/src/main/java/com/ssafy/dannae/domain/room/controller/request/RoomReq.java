package com.ssafy.dannae.domain.room.controller.request;

import lombok.Builder;

@Builder
public record RoomReq(
	String title,
	String mode,
	Boolean release
) {
}
