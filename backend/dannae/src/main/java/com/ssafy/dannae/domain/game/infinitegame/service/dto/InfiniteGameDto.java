package com.ssafy.dannae.domain.game.infinitegame.service.dto;

import java.util.List;

import lombok.Builder;

@Builder
public record InfiniteGameDto(
	String word,
	Long roomId,
	Long gameId,
	String initial,
	List<String> meaning,
	Boolean correct,
	Integer difficulty,
	Long playerId

) {
}
