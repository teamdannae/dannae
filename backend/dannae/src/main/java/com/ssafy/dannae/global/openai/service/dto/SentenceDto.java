package com.ssafy.dannae.global.openai.service.dto;

import java.util.List;
import java.util.Set;

import lombok.Builder;

@Builder
public record SentenceDto(
		Set<String> activeWords,
		List<Long> playerIds,
		List<String> sentences
) {
}
