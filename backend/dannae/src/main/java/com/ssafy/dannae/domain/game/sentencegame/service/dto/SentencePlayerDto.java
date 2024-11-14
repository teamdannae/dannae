package com.ssafy.dannae.domain.game.sentencegame.service.dto;

import lombok.Builder;

import java.util.Set;

@Builder
public record SentencePlayerDto(
	Long playerId,
	Integer playerCorrects,
	Integer playerNowScore,
	Long playerTotalScore,
	String playerSentence

) {
}
