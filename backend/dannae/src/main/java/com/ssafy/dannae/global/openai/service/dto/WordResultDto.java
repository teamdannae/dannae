package com.ssafy.dannae.global.openai.service.dto;

import java.util.List;
import java.util.Set;

import lombok.Builder;

@Builder
public record WordResultDto(
	List<Integer> correctNum,
	List<Integer> playerScore,
	List<Long> playerTotalScore,
	Set<String> usedWords
) {
}
