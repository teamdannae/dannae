package com.ssafy.dannae.domain.game.sentencegame.controller.response;

import com.ssafy.dannae.domain.game.sentencegame.service.dto.SentencePlayerDto;
import lombok.Builder;

import java.util.List;
import java.util.Set;

@Builder
public record SentenceGameRes(
	Boolean isEnd,
	Set<String> userWords,
	List<SentencePlayerDto> playerDtos
) {
}
