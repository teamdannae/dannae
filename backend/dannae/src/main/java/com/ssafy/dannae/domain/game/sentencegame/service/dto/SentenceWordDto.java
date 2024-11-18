package com.ssafy.dannae.domain.game.sentencegame.service.dto;

import lombok.Builder;

import java.util.Set;

@Builder
public record SentenceWordDto(
	String word,
	int difficulty

) {
}
