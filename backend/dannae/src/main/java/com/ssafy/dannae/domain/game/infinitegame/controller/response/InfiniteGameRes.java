package com.ssafy.dannae.domain.game.infinitegame.controller.response;

import java.util.List;

import lombok.Builder;

@Builder
public record InfiniteGameRes(
	Boolean correct,
	String word,
	List<String> meaning,
	Integer difficulty
) {
}
