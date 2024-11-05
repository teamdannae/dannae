package com.ssafy.dannae.domain.game.infinitegame.controller.response;

import lombok.Builder;

@Builder
public record InfiniteGameCreateRes(
	String initial,
	Long id
) {
}
