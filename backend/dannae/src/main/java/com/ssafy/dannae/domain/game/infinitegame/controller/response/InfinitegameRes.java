package com.ssafy.dannae.domain.game.infinitegame.controller.response;

import lombok.Builder;

@Builder
public record InfinitegameRes(
	Boolean correct,
	String word,
	String meaning,
	Integer difficulty
) {
}
