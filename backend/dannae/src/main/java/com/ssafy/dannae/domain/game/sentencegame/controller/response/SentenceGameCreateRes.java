package com.ssafy.dannae.domain.game.sentencegame.controller.response;

import lombok.Builder;

import java.util.Set;

@Builder
public record SentenceGameCreateRes(
	Long roomId,
	Set<String> activeWords,
	Set<String> inactiveWords
) {
}
