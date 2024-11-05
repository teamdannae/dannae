package com.ssafy.dannae.domain.game.infinitegame.service.dto;

import lombok.Builder;

@Builder
public record InfinitegameDto(
	String word,
	Long roomId,
	Long gameId,
	String initial,
	String meaning,
	Boolean correct,
	Integer difficulty,
	Long playerId

) {
}
