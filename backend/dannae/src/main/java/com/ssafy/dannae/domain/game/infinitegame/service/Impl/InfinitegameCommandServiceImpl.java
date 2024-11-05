package com.ssafy.dannae.domain.game.infinitegame.service.Impl;

import java.util.Optional;
import java.util.Random;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.dannae.domain.game.entity.Word;
import com.ssafy.dannae.domain.game.infinitegame.entity.Infinitegame;
import com.ssafy.dannae.domain.game.infinitegame.repository.InfinitegameRepository;
import com.ssafy.dannae.domain.game.infinitegame.service.InfinitegameCommandService;
import com.ssafy.dannae.domain.game.infinitegame.service.dto.InfinitegameDto;
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
public class InfinitegameCommandServiceImpl implements InfinitegameCommandService {

	private final InfinitegameRepository infinitegameRepository;
	private final WordRepository wordRepository;
	private final PlayerRepository playerRepository;

	private static final char[] CHO_SUNG = {
		'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ',
		'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
	};

	private static final int[] score = {
		10, 20, 40, 60
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

	/**
	 * 랜덤 초성을 만들어서 방 번호와 함께 반환해주는 메서드
	 * @param infinitegameDto
	 * @return
	 */
	@Override
	public InfinitegameDto createInitial(InfinitegameDto infinitegameDto) {

		String initial = randomInitial();
		Infinitegame infinitegame = Infinitegame.builder()
			.roomId(infinitegameDto.roomId())
			.initial(initial)
			.build();

		Infinitegame createInfinitegame = infinitegameRepository.save(infinitegame);

		InfinitegameDto res = InfinitegameDto.builder()
			.initial(createInfinitegame.getInitial())
			.gameId(createInfinitegame.getId())
			.build();

		return res;
	}

	/**
	 * 단어가 의미가 맞고, db에 존재하는지 확인하기 위한 서비스
	 * @param infinitegameDto
	 * @return
	 */
	@Override
	public InfinitegameDto updateWord(InfinitegameDto infinitegameDto) {

		InfinitegameDto res;

		if(!checkInitial(infinitegameDto.initial(), infinitegameDto.word())) {
			res = InfinitegameDto.builder()
				.correct(false)
				.meaning("초성에 맞지 않은 단어입니다.")
				.build();
			return res;
		}

		Infinitegame infinitegame = infinitegameRepository.findById(infinitegameDto.gameId())
			.orElseThrow(() -> new NoRoomException("게임방이 존재하지 않습니다."));

		for(String word : infinitegame.getList()){
			if(word.equals(infinitegameDto.word())){
				return InfinitegameDto.builder()
					.correct(false)
					.meaning("이미 사용된 단어입니다.")
					.build();
			}
		}

		Optional<Word> optionalWord = wordRepository.findByInitialAndWord(infinitegameDto.initial(), infinitegameDto.word());

		if (optionalWord.isEmpty()) {
			return InfinitegameDto.builder()
				.correct(false)
				.meaning("존재하지 않는 단어입니다.")
				.build();
		}

		Word word = optionalWord.get();

		word.updateInitialCount();
		wordRepository.save(word);

		infinitegame.updateList(word.getWord());
		infinitegameRepository.save(infinitegame);

		Player player = playerRepository.findById(infinitegameDto.playerId()).get();
		player.updateScore(score[word.getDifficulty()]);
		playerRepository.save(player);

		res = InfinitegameDto.builder()
			.correct(true)
			.word(word.getWord())
			.meaning(word.getMeaning())
			.difficulty(word.getDifficulty())
			.build();

		return res;
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
