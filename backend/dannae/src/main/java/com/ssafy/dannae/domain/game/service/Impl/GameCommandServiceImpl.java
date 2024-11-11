package com.ssafy.dannae.domain.game.service.Impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.dannae.domain.game.entity.Word;
import com.ssafy.dannae.domain.game.exception.NoWordException;
import com.ssafy.dannae.domain.game.repository.WordRepository;
import com.ssafy.dannae.domain.game.service.GameCommandService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
class GameCommandServiceImpl implements GameCommandService {

	private final WordRepository wordRepository;

	@Override
	public List<String> readWordMeanings(String word){
		List<String> meanings = new ArrayList<>();
		List<Word> wordList = wordRepository.findByWord(word)
			.orElseThrow(() -> new NoWordException("Word not found"+word));
		for(Word wordInfo : wordList) {
			meanings.add(wordInfo.getMeaning());
		}
		return meanings;
	}

}
