package com.ssafy.dannae.domain.room.service.dto;

import lombok.Builder;

@Builder
public record RoomDetailDto(
	Long roomId,
	String title,
	String mode,
	Boolean release,
	Long playerCount,
	String code,
	Long creator
) {
}



