package com.ssafy.dannae.domain.game.infinitegame.service.Impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.dannae.domain.game.entity.Word;
import com.ssafy.dannae.domain.game.infinitegame.entity.InfiniteGame;
import com.ssafy.dannae.domain.game.infinitegame.repository.InfiniteGameRepository;
import com.ssafy.dannae.domain.game.infinitegame.service.InfiniteGameCommandService;
import com.ssafy.dannae.domain.game.infinitegame.service.dto.InfiniteGameDto;
import com.ssafy.dannae.domain.game.repository.WordRepository;
import com.ssafy.dannae.domain.player.entity.Player;
import com.ssafy.dannae.domain.player.repository.PlayerRepository;
import com.ssafy.dannae.domain.room.exception.NoRoomException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
class InfiniteGameCommandServiceImpl implements InfiniteGameCommandService {

	private static final char[] CHO_SUNG = {
		'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ',
		'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
	};
	private static final int[] score = {
		0, 10, 20, 40, 60, 100
	};
	private static final boolean[][] invalidCombinations = new boolean[19][19];

	static {
		int[] doubleConsonantIndexes = {1, 4, 8, 10, 13};

		for (int i : doubleConsonantIndexes) {
			for (int j : doubleConsonantIndexes) {
				invalidCombinations[i][j] = true;
			}
		}
	}

	private final InfiniteGameRepository infinitegameRepository;
	private final WordRepository wordRepository;
	private final PlayerRepository playerRepository;

	/**
	 * 랜덤 초성을 만들어서 방 번호와 함께 반환해주는 메서드
	 * @param infiniteGameDto
	 * @return
	 */
	@Override
	public InfiniteGameDto createInitial(InfiniteGameDto infiniteGameDto) {

		String initial = randomInitial();
		InfiniteGame infiniteGame = InfiniteGame.builder()
			.roomId(infiniteGameDto.roomId())
			.initial(initial)
			.build();

		InfiniteGame createInfiniteGame = infinitegameRepository.save(infiniteGame);

		InfiniteGameDto dto = InfiniteGameDto.builder()
			.initial(createInfiniteGame.getInitial())
			.gameId(createInfiniteGame.getId())
			.build();

		return dto;
	}

	/**
	 * 단어가 의미가 맞고, db에 존재하는지 확인하기 위한 서비스
	 * @param infiniteGameDto
	 * @return
	 */
	@Override
	public InfiniteGameDto updateWord(InfiniteGameDto infiniteGameDto) {

		InfiniteGameDto dto;

		if(!checkInitial(infiniteGameDto.initial(), infiniteGameDto.word())) {
			List<String> message = new ArrayList<>();
			message.add("초성에 맞지 않은 단어입니다.");
			dto = InfiniteGameDto.builder()
				.correct(false)
				.word(infiniteGameDto.word())
				.meaning(message)
				.build();
			return dto;
		}

		InfiniteGame infinitegame = infinitegameRepository.findById(infiniteGameDto.gameId())
			.orElseThrow(() -> new NoRoomException("게임방이 존재하지 않습니다."));

		for(String word : infinitegame.getList()){
			List<String> message = new ArrayList<>();
			message.add("이미 사용된 단어입니다.");
			if(word.equals(infiniteGameDto.word())){
				return InfiniteGameDto.builder()
					.correct(false)
					.word(infiniteGameDto.word())
					.meaning(message)
					.build();
			}
		}

		Optional<List<Word>> optionalWords = wordRepository.findAllByInitialAndWord(infiniteGameDto.initial(), infiniteGameDto.word());

		if (optionalWords.isEmpty() || optionalWords.get().isEmpty()) {
			List<String> message = new ArrayList<>();
			message.add("존재하지 않는 단어입니다.");
			return InfiniteGameDto.builder()
				.correct(false)
				.word(infiniteGameDto.word())
				.meaning(message)
				.build();
		}

		List<Word> words = optionalWords.get();
		List<String> meaning = new ArrayList<>();

		words.forEach(word -> {
			word.updateGameCount();
			wordRepository.save(word);
			meaning.add(word.getMeaning());
		});

		infinitegame.updateList(infiniteGameDto.word());
		infinitegameRepository.save(infinitegame);

		// 플레이어 점수 업데이트
		Player player = playerRepository.findById(infiniteGameDto.playerId()).orElseThrow(() ->
			new NoRoomException("플레이어를 찾을 수 없습니다.")
		);
		int totalScore = words.stream()
			.mapToInt(word -> score[word.getDifficulty()])
			.sum();
		player.updateScore(totalScore);
		playerRepository.save(player);

		Word easiestWord = words.stream()
			.min((w1, w2) -> Integer.compare(w1.getDifficulty(), w2.getDifficulty()))
			.orElse(words.get(0));

		return InfiniteGameDto.builder()
			.correct(true)
			.word(easiestWord.getWord())
			.meaning(meaning)
			.difficulty(easiestWord.getDifficulty())
			.build();
	}

	/**
	 * 랜덤한 초성을 만들어주는 메서드
	 * @return
	 */
	private String randomInitial(){

		String initial = "";

		while(true){
			Random random = new Random();

			int randomNumber1 = random.nextInt(19);
			int randomNumber2 = random.nextInt(19);

			if(!invalidCombinations[randomNumber1][randomNumber2]){
				initial = String.valueOf(CHO_SUNG[randomNumber1]) + CHO_SUNG[randomNumber2];
				break;
			}

		}

		return initial;

	}

	/**
	 * 초성에 맞는 단어인지 확인하는 메서드
	 * @return 초성에 맞는 단어면 true를, 초성에 맞지 않는 단어면 false를 반환.
	 */
	private boolean checkInitial(String initial, String word) {

		if (initial == null || word == null) {
			return false;
		}

		if (initial.isEmpty() || word.isEmpty() || initial.length() > word.length()) {
			return false;
		}

		for (int i = 0; i < initial.length(); i++) {
			char initialChar = initial.charAt(i);
			int initialIndex = -1;
			for (int j = 0; j < CHO_SUNG.length; j++) {
				if (CHO_SUNG[j] == initialChar) {
					initialIndex = j;
					break;
				}
			}

			if (initialIndex != extractInitialIndex(word.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 문자에서 초성을 추출하는 메서드
	 * @param c
	 * @return 유니코드를 활용해 초성 인덱스값 반환, 한글이 아닐 경우 -1 반환
	 */
	private int extractInitialIndex(char c) {
		if (c >= 0xAC00 && c <= 0xD7A3) {
			int unicodeValue = c - 0xAC00;
			return unicodeValue / (21 * 28);
		}

		for (int i = 0; i < CHO_SUNG.length; i++) {
			if (CHO_SUNG[i] == c) {
				return i;
			}
		}
		return -1;
	}

}
