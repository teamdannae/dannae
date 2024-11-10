package com.ssafy.dannae.domain.game.sentencegame.controller.response;

import lombok.Builder;

import java.util.List;
import java.util.Set;

@Builder
public record SentenceGameRes(
	Boolean isEnd,
	Set<String> activeWords,
	Set<String> inactiveWords,
	List<Integer> playerCorrects,
	List<Integer> playerNowScores,
	List<Long> playerTotalScores,
	List<String> playerSentences
) {
}
