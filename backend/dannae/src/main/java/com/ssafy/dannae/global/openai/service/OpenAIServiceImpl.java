package com.ssafy.dannae.global.openai.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.dannae.domain.game.entity.Word;
import com.ssafy.dannae.domain.game.exception.NoWordException;
import com.ssafy.dannae.domain.game.repository.WordRepository;
import com.ssafy.dannae.domain.player.entity.Player;
import com.ssafy.dannae.domain.player.exception.NoPlayerException;
import com.ssafy.dannae.domain.player.repository.PlayerRepository;
import com.ssafy.dannae.global.openai.exception.OpenAIAuthenticationException;
import com.ssafy.dannae.global.openai.exception.OpenAIRequestProcessingException;
import com.ssafy.dannae.global.openai.exception.OpenAIResponseProcessingException;
import com.ssafy.dannae.global.openai.exception.OpenAIServiceException;
import com.ssafy.dannae.global.openai.exception.OpenAITimeoutException;
import com.ssafy.dannae.global.openai.exception.OpenAITooManyRequestsException;
import com.ssafy.dannae.global.openai.service.dto.PromptDto;
import com.ssafy.dannae.global.openai.service.dto.SentenceDto;
import com.ssafy.dannae.global.openai.service.dto.WordResultDto;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Transactional
@Service
class OpenAIServiceImpl implements OpenAIService {

	private final ObjectMapper objectMapper;
	private final RestClient restClient;
	private final WordRepository wordRepository;
	private final PlayerRepository playerRepository;

	@Value("${openai.model}")
	private String model;

	@Value("${openai.api.key}")
	private String apiKey;

	/**
	 * OpenAI API에 프롬프트를 전송하고, 결과 문자열을 반환합니다.
	 *
	 * @param promptDto OpenAI에 전송할 프롬프트 정보를 담은 DTO
	 * @return OpenAI API로부터의 응답 문자열
	 * @throws OpenAIRequestProcessingException 요청 생성 과정에서 JSON 처리에 실패한 경우 예외를 발생시킵니다.
	 */
	@Override
	public String prompt(PromptDto promptDto) {
		try {
			List<Map<String, String>> messages = List.of(
				Map.of("role", "user", "content", promptDto.messages().get(0).get("content"))
			);

			PromptDto requestDto = PromptDto.builder()
				.model(model)
				.messages(messages)
				.temperature(promptDto.temperature())
				.max_tokens(promptDto.max_tokens())
				.build();

			String requestBody = objectMapper.writeValueAsString(requestDto);
			return executeWithRetry(requestBody);
		} catch (JsonProcessingException e) {
			throw new OpenAIRequestProcessingException("Failed to process request body", e);
		}
	}

	/**
	 * OpenAI API에 요청을 보내고 응답을 받습니다.
	 *
	 * @param requestBody OpenAI API에 보낼 요청 본문
	 * @return OpenAI API의 응답 문자열
	 * @throws OpenAIAuthenticationException 인증 실패 시 예외를 발생시킵니다.
	 * @throws OpenAITooManyRequestsException 요청이 과도한 경우 예외를 발생시킵니다.
	 * @throws OpenAITimeoutException 요청 시간 초과 시 예외를 발생시킵니다.
	 * @throws OpenAIResponseProcessingException 응답 처리 중 JSON 처리에 실패한 경우 예외를 발생시킵니다.
	 * @throws OpenAIServiceException 예기치 못한 에러가 발생한 경우 예외를 발생시킵니다.
	 */
	private String sendRequest(String requestBody) throws JsonProcessingException {
		try {
			ResponseEntity<String> response = restClient
				.post()
				.uri("/chat/completions")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
				.body(requestBody)
				.retrieve()
				.toEntity(String.class);

			JsonNode responseJson = objectMapper.readTree(response.getBody());
			return responseJson.at("/choices/0/message/content").asText().trim();

		} catch (HttpClientErrorException e) {
			HttpStatusCode statusCode = e.getStatusCode();

			if (statusCode.value() == 401) {
				throw new OpenAIAuthenticationException("Authentication failed", e);
			} else if (statusCode.value() == 429) {
				throw new OpenAITooManyRequestsException("Rate limit exceeded", e);
			}

			throw new OpenAIServiceException("Failed to process request", e);
		} catch (ResourceAccessException e) {
			throw new OpenAITimeoutException("Request timed out", e);
		} catch (JsonProcessingException e) {
			throw new OpenAIResponseProcessingException("Failed to process API response", e);
		} catch (Exception e) {
			throw new OpenAIServiceException("Unexpected error occurred", e);
		}
	}

	/**
	 * 요청 실패 시 재시도하며 OpenAI API에 요청을 보냅니다.
	 *
	 * @param requestBody OpenAI API에 보낼 요청 본문
	 * @return OpenAI API의 응답 문자열
	 * @throws OpenAIServiceException 최대 재시도 횟수를 초과하거나 중단된 경우 예외를 발생시킵니다.
	 */
	private String executeWithRetry(String requestBody) {
		int attempts = 0;
		int maxAttempts = 5;
		long backoffTime = 1000;

		while (attempts < maxAttempts) {
			try {
				return sendRequest(requestBody);
			} catch (OpenAITooManyRequestsException e) {
				attempts++;
				if (attempts >= maxAttempts) {
					throw new OpenAITooManyRequestsException(
						"Too many requests, please try again later", e);
				}
				try {
					Thread.sleep(backoffTime);
					backoffTime *= 2;
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					throw new OpenAIServiceException("Retry operation was interrupted", ie);
				}
			} catch (OpenAITimeoutException | OpenAIAuthenticationException e) {
				throw e;
			} catch (Exception e) {
				throw new OpenAIServiceException("Unexpected error occurred", e);
			}
		}
		throw new OpenAIServiceException("Failed to process request after maximum retry attempts");
	}

	/**
	 * 문장의 유효성 여부와 사용된 단어들을 확인하고 결과를 반환하는 메서드.
	 *
	 * @param sentenceDto SentenceDto 객체로, 활성 단어 목록, 플레이어 ID 목록, 문장 목록을 포함합니다.
	 * @return WordResultDto 객체로, 각 문장에 대해 적절히 사용된 단어 개수 리스트와 모든 플레이어가 사용한 단어 집합을 반환합니다.
	 * @throws OpenAIResponseProcessingException OpenAI 응답 처리 중 오류가 발생한 경우 예외를 발생시킵니다.
	 */
	@Override
	public WordResultDto wordResult(SentenceDto sentenceDto) {
		Set<String> wordList = sentenceDto.activeWords();
		List<Long> playerList = sentenceDto.playerIds();
		List<String> sentenceList = sentenceDto.sentences();
		List<Integer> usedWordCount = new ArrayList<>(); // 각 문장에서 사용된 단어 개수 리스트
		Set<String> usedSentence = new HashSet<>(); // 모든 플레이어가 사용한 단어 집합
		List<Integer> playerScore = new ArrayList<>(); // 각 플레이어의 점수 리스트
		List<Long> playerTotalScore = new ArrayList<>(); // 각 플레이어의 총 점수 리스트

		for (int i = 0; i < playerList.size(); i++) {
			Long playerId = playerList.get(i);
			Player nowPlayer = playerRepository.findById(playerId)
				.orElseThrow(() -> new NoPlayerException("Player not found with ID: " + playerId));
			String playerSentence = sentenceList.get(i);

			String wordQuestion = "다음 문장이 유효한 문장인지 판단해보세요 \n"
				+ "문장: \"" + playerSentence + "\"\n"
				+ "유효하지 않은 문장이라면 null 반환해주세요 \n"
				+ "문장이 아니라 단어만 입력했을 때도 null 반환해주세요 \n"
				+ "유효한 문장이라면 주어진 단어들 중 문장에서 적절하게 사용된 단어들을 반환해주세요.\n"
				+ "적절하게 사용된 단어가 없다면 null 반환해주세요.\n"
				+ "단어 목록: \"" + wordList.toString() + "\"\n"
				+ "무조건 단어 목록에 있는 단어만 가져와야합니다. \n"
				+ "답변 형식: [\"단어1\", \"단어2\", ...] \n"
				+ "형식에 맞게 큰따음표까지 모두 반환해야합니다 \n"
				+ "과정 설명 없이 답변 형식에 맞게 단어 목록만 제공해주세요.\n";

			PromptDto sentencePrompt = PromptDto.builder()
				.messages(List.of(Map.of("role", "user", "content", wordQuestion)))
				.temperature(0.8f)
				.max_tokens(1000)
				.build();

			String sentenceResult = prompt(sentencePrompt);

			if (sentenceResult == null || sentenceResult.isEmpty() || sentenceResult.contains("null")) {
				usedWordCount.add(0);
			} else {
				try {
					// 대괄호와 따옴표를 제거하여 단어 리스트만 추출
					String words = sentenceResult.replaceAll("[\\[\\]\"]", "").trim();
					int count = 0;
					int scoreCount = 0;

					if (!words.equals("null") && !words.isEmpty()) {
						String[] wordArray = words.split(", ");

						for (String word : wordArray) {
							if (wordList.contains(word.trim())) { // 단어 목록에 포함된 단어인지 확인
								Word usedWord = wordRepository.findFirstByWord(word)
									.orElseThrow(() -> new NoWordException("Word not found : " + word));
								count++; // 사용된 단어 개수 증가
								int difficultyScore = usedWord.getDifficulty()*100;
								nowPlayer.updateScore(difficultyScore);
								scoreCount += difficultyScore;
								usedSentence.add(word.trim()); // 사용된 단어 집합에 추가
							}
						}
					}
					usedWordCount.add(count); // 사용된 단어 개수 추가
					playerScore.add(scoreCount); // 플레이어 점수 추가
					playerTotalScore.add(nowPlayer.getScore()); // 플레이어 총 점수 추가
				} catch (Exception e) {
					throw new OpenAIResponseProcessingException("Failed to process sentence result: " + sentenceResult, e);
				}
			}
		}
		return WordResultDto.builder()
			.correctNum(usedWordCount)
			.playerScore(playerScore)
			.playerTotalScore(playerTotalScore)
			.usedWords(usedSentence)
			.build();
	}


}

