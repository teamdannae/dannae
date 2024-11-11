package com.ssafy.dannae.domain.game.sentencegame.service.Impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.dannae.domain.game.entity.Word;
import com.ssafy.dannae.domain.game.exception.NoWordException;
import com.ssafy.dannae.domain.game.repository.WordRepository;
import com.ssafy.dannae.domain.game.sentencegame.controller.request.SentenceGameReq;
import com.ssafy.dannae.domain.game.sentencegame.controller.response.SentenceGameRes;
import com.ssafy.dannae.domain.game.sentencegame.entity.SentenceGame;
import com.ssafy.dannae.domain.game.sentencegame.factory.SentenceGameFactory;
import com.ssafy.dannae.domain.game.sentencegame.repository.SentenceGameRepository;
import com.ssafy.dannae.domain.game.sentencegame.service.SentenceGameCommandService;
import com.ssafy.dannae.domain.game.sentencegame.service.dto.SentenceGameDto;
import com.ssafy.dannae.domain.game.sentencegame.service.dto.SentencePlayerDto;
import com.ssafy.dannae.domain.player.entity.Player;
import com.ssafy.dannae.domain.player.exception.NoPlayerException;
import com.ssafy.dannae.domain.player.repository.PlayerRepository;
import com.ssafy.dannae.domain.room.exception.NoRoomException;
import com.ssafy.dannae.global.openai.service.OpenAIService;
import com.ssafy.dannae.global.openai.service.dto.SentenceDto;
import com.ssafy.dannae.global.openai.service.dto.WordResultDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
class SentenceGameCommandServiceImpl implements SentenceGameCommandService {

	private static final int[] scores = {
			0, 10, 20, 30, 40, 60,
			80, 100, 130, 160, 200,
			250, 300, 350, 400, 450,
			500, 550, 600, 650, 700,
			750, 800, 850, 900, 950,
			1000, 1050, 1100, 1150, 1200
	};
	private final SentenceGameRepository sentenceGameRepository;
	private final SentenceGameFactory sentenceGameFactory;
	private final WordRepository wordRepository;
	private final PlayerRepository playerRepository;
	private final OpenAIService openAIService;

	/**
	 * 랜덤 단어셋을 만들어 방 번호와 함께 반환해주는 메서드
	 * @param sentenceGameDto
	 * @return sentenceGameRes
	 */
	@Override
	public SentenceGameDto createInitial(SentenceGameDto sentenceGameDto) {

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
	 * 입력된 문장을 확인하고 점수와 사용한 단어를 반환하는 메서드
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

		List<SentencePlayerDto> dtos = new ArrayList<>();
		boolean isEnd = false;
		int playerNum = sentenceGameReq.players().size();
		// 아무도 단어를 쓰지 못했을 때
		if(resultDto.usedWords().isEmpty()){
			for(int i=0; i<playerNum; i++){
				Player player = playerRepository.findById(
						sentenceGameReq.players().get(i)
				).orElseThrow(()-> new NoPlayerException("유저를 찾을 수 없습니다"));
				dtos.add(new SentencePlayerDto(player.getId(),
						0, 0, player.getScore(), ""));
				player.resetScore();
			}
			updateWordCount(sentenceGame.getInactiveWords());
			SentenceGameRes res = SentenceGameRes.builder().isEnd(true)
					.userWords(new HashSet<>())
					.playerDtos(dtos)
					.build();
			sentenceGameRepository.deleteById(sentenceGameReq.roomId());
			return res;
		}

		for(int i=0; i<playerNum; i++){
			Player player = playerRepository.findById(
					sentenceGameReq.players().get(i)
			).orElseThrow(()-> new NoPlayerException("유저를 찾을 수 없습니다"));
			int correctCnt = resultDto.correctNum().get(i);
			player.updateScore(scores[correctCnt]);
			dtos.add(new SentencePlayerDto(player.getId(),
					correctCnt, scores[correctCnt], player.getScore(),
					sentenceGameReq.sentences().get(i)));
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
		}

		SentenceGameRes res = SentenceGameRes.builder().isEnd(isEnd)
				.playerDtos(dtos)
				.userWords(resultDto.usedWords())
				.build();

		if(isEnd){
			updateWordCount(inactiveWords);
			for(int i=0; i<playerNum; i++){
				Player player = playerRepository.findById(sentenceGameReq.players().get(i)).get();
				player.resetScore();
			}
			sentenceGameRepository.deleteById(sentenceGameReq.roomId());
		}

		return res;
	}

	/**
	 * 게임 종료시 사용된 단어의 사용 횟수를 업데이트하는 메서드
	 */
	@Override
	public void updateWordCount(Set<String> wordSet) {
		for(String string: wordSet){
			Word word = wordRepository.findFirstByWord(string)
					.orElseThrow(()-> new NoWordException(string+ " 단어가 없습니다"));
			word.updateGameCount();
		}
	}

}
