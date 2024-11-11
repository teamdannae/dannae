package com.ssafy.dannae.domain.game.sentencegame.service;

import com.ssafy.dannae.domain.game.sentencegame.controller.request.SentenceGameReq;
import com.ssafy.dannae.domain.game.sentencegame.controller.response.SentenceGameRes;
import com.ssafy.dannae.domain.game.sentencegame.service.dto.SentenceGameDto;
import org.springframework.stereotype.Service;

import java.util.Set;


public interface SentenceGameCommandService {

	/**
	 * 랜덤 단어셋을 만들어 방 번호와 함께 반환해주는 메서드
	 * @param sentenceGameDto
	 * @return
	 */
	SentenceGameDto createInitial(SentenceGameDto sentenceGameDto);

	/**
	 * 입력된 문장을 확인하고 점수와 사용한 단어를 반환하는 메서드
	 * @param sentenceGameReq
	 * @return
	 */
	SentenceGameRes playGame(SentenceGameReq sentenceGameReq);

	void updateWordCount(Set<String> wordSet);

}
