package com.ssafy.dannae.domain.game.sentencegame.service.Impl;

import com.ssafy.dannae.domain.game.entity.Word;
import com.ssafy.dannae.domain.game.repository.WordRepository;
import com.ssafy.dannae.domain.game.sentencegame.controller.request.SentenceGameReq;
import com.ssafy.dannae.domain.game.sentencegame.controller.response.SentenceGameRes;
import com.ssafy.dannae.domain.game.sentencegame.entity.SentenceGame;
import com.ssafy.dannae.domain.game.sentencegame.factory.SentenceGameFactory;
import com.ssafy.dannae.domain.game.sentencegame.repository.SentenceGameRepository;
import com.ssafy.dannae.domain.game.sentencegame.service.SentenceGameCommandService;
import com.ssafy.dannae.domain.game.sentencegame.service.dto.SentenceGameDto;
import com.ssafy.dannae.domain.player.entity.Player;
import com.ssafy.dannae.domain.player.repository.PlayerRepository;
import com.ssafy.dannae.domain.room.exception.NoRoomException;
import com.ssafy.dannae.global.openai.service.OpenAIService;
import com.ssafy.dannae.global.openai.service.dto.SentenceDto;
import com.ssafy.dannae.global.openai.service.dto.WordResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
class SentenceGameCommandServiceImpl implements SentenceGameCommandService {

	private final SentenceGameRepository sentenceGameRepository;
	private final SentenceGameFactory sentenceGameFactory;
	private final WordRepository wordRepository;
	private final PlayerRepository playerRepository;
	private final OpenAIService openAIService;



	/**
	 * 랜덤 단어셋을 만들어 방 번호와 함께 반환해주는 메서드
	 * @param sentenceGameDto
	 * @return
	 */
	@Override
	public SentenceGameDto createInitial(SentenceGameDto sentenceGameDto) {

		List<Word> initial = wordRepository.findRandomWords();
		SentenceGame sentenceGame = sentenceGameFactory.createSentenceGame(
				sentenceGameDto.roomId());
		sentenceGameRepository.save(sentenceGame);

		return SentenceGameDto.builder()
				.roomId(sentenceGame.getRoomId())
				.activeWords(sentenceGame.getActiveWords())
				.inactiveWords(sentenceGame.getInactiveWords())
				.build();
	}

	/**
	 * 단어가 의미가 맞고, db에 존재하는지 확인하기 위한 서비스
	 * @param sentenceGameReq
	 * @return sentenceGameRes
	 */
	@Override
	public SentenceGameRes playGame(SentenceGameReq sentenceGameReq) {

		SentenceGame sentenceGame = sentenceGameRepository.findById(sentenceGameReq.roomId())
				.orElseThrow(() -> new NoRoomException("게임방이 존재하지 않습니다."));

		SentenceDto sentenceDto = new SentenceDto(sentenceGame.getActiveWords(),
				sentenceGameReq.players(),
				sentenceGameReq.sentences());

		WordResultDto resultDto = openAIService.wordResult(sentenceDto);

		List<Integer> playerCorrects = new ArrayList<>();
		List<Long> playerScores = new ArrayList<>();
		boolean isEnd = false;
		// 아무도 단어를 쓰지 못했을 때
		if(resultDto.usedWords().isEmpty()){
			for(int i=0; i<4; i++){
				playerCorrects.add(resultDto.correctNum().get(i));
				playerScores.add(playerRepository.findById(
						sentenceGameReq.players().get(i)
				).get().getScore());
			}
			sentenceGameRepository.deleteById(sentenceGameReq.roomId());
			return SentenceGameRes.builder().isEnd(true)
					.inactiveWords(sentenceGame.getInactiveWords())
					.playerCorrects(playerCorrects)
					.playerScore(playerScores)
					.playerSentences(sentenceGameReq.sentences())
					.build();
		}

		for(int i=0; i<4; i++){
			Player player = playerRepository.findById(sentenceGameReq.players().get(i)).get();
			int correctCnt = resultDto.correctNum().get(i);
			player.updateScore(correctCnt);
			playerCorrects.add(correctCnt);
			playerScores.add(player.getScore());
		}

		Set<String> activeWords = sentenceGame.getActiveWords();
		Set<String> inactiveWords = sentenceGame.getInactiveWords();
		for(String word: resultDto.usedWords()){
			activeWords.remove(word);
			inactiveWords.add(word);
		}

		// 단어를 모두 소진했을 때
		if(activeWords.isEmpty()){
			isEnd = true;
			sentenceGameRepository.deleteById(sentenceGameReq.roomId());
		}

		return SentenceGameRes.builder().isEnd(isEnd)
				.playerSentences(sentenceGameReq.sentences())
				.playerScore(playerScores)
				.playerCorrects(playerCorrects)
				.activeWords(activeWords)
				.inactiveWords(inactiveWords)
				.build();
	}

}
