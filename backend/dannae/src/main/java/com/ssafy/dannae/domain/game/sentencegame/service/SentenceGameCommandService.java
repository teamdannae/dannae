package com.ssafy.dannae.domain.game.sentencegame.service;

import com.ssafy.dannae.domain.game.sentencegame.controller.request.SentenceGameReq;
import com.ssafy.dannae.domain.game.sentencegame.controller.response.SentenceGameRes;
import com.ssafy.dannae.domain.game.sentencegame.service.dto.SentenceGameDto;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public interface SentenceGameCommandService {

	/**
	 * 게임에 사용할 초성을 생성하고, 게임에 사용할 객체를 생성해주는 서비스.
	 * @param sentenceGameDto
	 * @return
	 */
	SentenceGameDto createInitial(SentenceGameDto sentenceGameDto);

	/**
	 * 입력된 단어를 확인하고 올바른 단어인지 확인하는 서비스.
	 * @param sentenceGameReq
	 * @return
	 */
	SentenceGameRes playGame(SentenceGameReq sentenceGameReq);

	void updateWordCount(Set<String> wordSet);

}
