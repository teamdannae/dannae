package com.ssafy.dannae.domain.game.sentencegame.controller.request;

import lombok.Builder;

import java.util.List;

@Builder
public record SentenceGameReq(
	Long roomId,
	List<Long> players,
	List<String> sentences
) {
}
