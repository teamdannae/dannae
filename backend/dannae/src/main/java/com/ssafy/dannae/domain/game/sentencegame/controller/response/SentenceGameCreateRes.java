package com.ssafy.dannae.domain.game.sentencegame.controller.response;

import com.ssafy.dannae.domain.game.sentencegame.service.dto.SentenceWordDto;
import lombok.Builder;

import java.util.List;

@Builder
public record SentenceGameCreateRes(
	Long roomId,
	List<SentenceWordDto> words
) {
}
