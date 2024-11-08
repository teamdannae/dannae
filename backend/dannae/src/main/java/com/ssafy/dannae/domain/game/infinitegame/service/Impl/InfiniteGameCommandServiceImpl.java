package com.ssafy.dannae.domain.game.infinitegame.service.Impl;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

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
	@Value("${korean.api.url}")
	private String apiUrl;
	@Value("${korean.api.key}")
	private String apiKey;

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
			List<String> message = List.of("초성에 맞지 않은 단어입니다.");
			dto = InfiniteGameDto.builder()
				.correct(false)
				.word(infiniteGameDto.word())
				.meaning(message)
				.build();
			return dto;
		}

		InfiniteGame infinitegame = infinitegameRepository.findById(infiniteGameDto.gameId())
			.orElseThrow(() -> new NoRoomException("게임방이 존재하지 않습니다."));

		if (infinitegame.getList().stream().anyMatch(word -> word.equals(infiniteGameDto.word()))) {
			List<String> message = List.of("이미 사용된 단어입니다.");
			return InfiniteGameDto.builder()
				.correct(false)
				.word(infiniteGameDto.word())
				.meaning(message)
				.build();
		}

		Optional<List<Word>> optionalWords = wordRepository.findAllByInitialAndWord(infiniteGameDto.initial(), infiniteGameDto.word());

		List<Word> words;
		if (optionalWords.isEmpty() || optionalWords.get().isEmpty()) {
			words = fetchWordsFromKoreanApi(infiniteGameDto.word());

			if (words.isEmpty()) {
				List<String> message = List.of("존재하지 않는 단어입니다.");
				return InfiniteGameDto.builder()
					.correct(false)
					.word(infiniteGameDto.word())
					.meaning(message)
					.build();
			}

			wordRepository.saveAll(words);
		} else {
			words = optionalWords.get();
		}

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

	/**
	 * 국립국어원 API를 호출하여 단어와 뜻을 가져오는 메서드
	 * @param word 검색할 단어
	 * @return
	 */
	private List<Word> fetchWordsFromKoreanApi(String word) {
		List<Word> words = new ArrayList<>();

		try {
			// API 요청 URL 구성
			String requestUrl = apiUrl + "?key=" + apiKey
				+ "&q=" + URLEncoder.encode(word, "UTF-8")
				+ "&req_type=xml"      // XML 형식 요청
				+ "&start=1"            // 검색의 시작 번호
				+ "&num=100"             // 결과 출력 건수
				+ "&advanced=n";        // 자세히 찾기 미사용

			// HTTP 요청 수행
			HttpURLConnection connection = (HttpURLConnection) new URL(requestUrl).openConnection();
			connection.setRequestMethod("GET");

			if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
				InputStream responseStream = connection.getInputStream();
				String response = new BufferedReader(new InputStreamReader(responseStream))
					.lines().collect(Collectors.joining("\n"));

				// XML 파싱
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = factory.newDocumentBuilder();
				Document document = builder.parse(new InputSource(new StringReader(response)));

				NodeList itemList = document.getElementsByTagName("item");

				for (int i = 0; i < itemList.getLength(); i++) {
					Node item = itemList.item(i);
					if (item.getNodeType() == Node.ELEMENT_NODE) {
						Element element = (Element) item;
						String wordText = element.getElementsByTagName("word").item(0).getTextContent().trim();
						String meaning = element.getElementsByTagName("definition").item(0).getTextContent().trim();

						// 콘솔 출력 - API 응답이 제대로 가져와졌는지 확인하기 위함
						System.out.println("Word: " + wordText);
						System.out.println("Meaning: " + meaning);

						// 초성 추출
						String initial = "";
						for (char c : wordText.toCharArray()) {
							int initialIndex = extractInitialIndex(c); // extractInitialIndex 호출
							if (initialIndex != -1) {
								initial += CHO_SUNG[initialIndex]; // 초성 추가
							}
						}

						// Word 객체 생성 및 리스트에 추가
						Word newWord = Word.builder()
							.word(wordText)
							.meaning(meaning)
							.initial(initial)
							.build();

						words.add(newWord);
					}
				}

				// 단어가 발견되면 데이터베이스에 추가
				if (!words.isEmpty()) {
					wordRepository.saveAll(words);
				}
			} else {
				System.out.println("API 요청 실패. 응답 코드: " + connection.getResponseCode());
			}
		} catch (Exception e) {
			log.error("Failed to fetch words from Korean API: {}", e.getMessage(), e);
		}

		return words;
	}

}
