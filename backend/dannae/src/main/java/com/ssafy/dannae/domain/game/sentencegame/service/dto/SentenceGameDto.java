package com.ssafy.dannae.domain.game.sentencegame.service.dto;

import lombok.Builder;

import java.util.Set;

@Builder
public record SentenceGameDto(
	Long roomId,
	Set<String> activeWords,
	Set<String> inactiveWords

) {
}
