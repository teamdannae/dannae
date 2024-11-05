package com.ssafy.dannae.domain.game.infinitegame.controller.request;

import lombok.Builder;

@Builder
public record InfiniteGameReq(
	String word,
	Long gameId,
	String initial,
	Long playerId
) {
}
