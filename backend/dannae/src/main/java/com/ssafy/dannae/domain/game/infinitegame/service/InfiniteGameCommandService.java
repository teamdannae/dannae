package com.ssafy.dannae.domain.game.infinitegame.service;

import org.springframework.stereotype.Service;

import com.ssafy.dannae.domain.game.infinitegame.service.dto.InfiniteGameDto;

@Service
public interface InfiniteGameCommandService {

	/**
	 * 게임에 사용할 초성을 생성하고, 게임에 사용할 객체를 생성해주는 서비스.
	 * @param infinitegameDto
	 * @return
	 */
	InfiniteGameDto createInitial(InfiniteGameDto infinitegameDto);

	/**
	 * 입력된 단어를 확인하고 올바른 단어인지 확인하는 서비스.
	 * @param infinitegameDto
	 * @return
	 */
	InfiniteGameDto updateWord(InfiniteGameDto infinitegameDto);



}
