package com.ssafy.dannae.domain.room.service.dto;

import com.ssafy.dannae.domain.room.entity.RoomStatus;

import lombok.Builder;

@Builder
public record RoomDto(
	Long roomId,
	String title,
	String mode,
	Long playerCount,
	Long creator,
	String creatorNickname,
	RoomStatus status
) {
}
