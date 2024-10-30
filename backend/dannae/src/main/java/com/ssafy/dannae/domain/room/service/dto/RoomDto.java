package com.ssafy.dannae.domain.room.service.dto;

import lombok.Builder;

@Builder
public record RoomDto(
	Long roomId,
	String title,
	String mode,
	String release
) {
}
