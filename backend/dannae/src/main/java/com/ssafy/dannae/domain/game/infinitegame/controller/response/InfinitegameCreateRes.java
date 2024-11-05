package com.ssafy.dannae.domain.game.infinitegame.controller.response;

import lombok.Builder;

@Builder
public record InfinitegameCreateRes(
	String initial,
	Long id
) {
}
