package com.ssafy.dannae.global.openai.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.dannae.global.openai.config.OpenAIConfig;
import com.ssafy.dannae.global.openai.service.dto.PromptDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
class OpenAiServiceImpl implements OpenAIService {

	private final OpenAIConfig openAIConfig;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Value("${openai.model}")
	private String model;

	@Value("${openai.api.key}")
	private String apiKey;

	@Override
	public String prompt(PromptDto promptDto) {

		HttpHeaders headers = openAIConfig.httpHeaders();
		headers.set("Authorization", "Bearer " + apiKey);

		List<Map<String, String>> messages = List.of(
			Map.of("role", "user", "content", promptDto.messages().get(0).get("content"))
		);

		PromptDto requestDto = PromptDto.builder()
			.model(model)
			.messages(messages)
			.temperature(promptDto.temperature())
			.max_tokens(promptDto.max_tokens())
			.build();

		String requestBody;
		try {
			requestBody = objectMapper.writeValueAsString(requestDto);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}

		HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

		int attempts = 0;
		int maxAttempts = 5;
		int backoffTime = 1000;

		while (attempts < maxAttempts) {
			try {
				ResponseEntity<String> response = openAIConfig.restTemplate()
					.exchange("https://api.openai.com/v1/chat/completions", HttpMethod.POST, requestEntity, String.class);

				JsonNode responseJson = objectMapper.readTree(response.getBody());
				return responseJson.at("/choices/0/message/content").asText().trim();
			} catch (HttpClientErrorException.TooManyRequests e) {
				attempts++;
				try {
					Thread.sleep(backoffTime);
				} catch (InterruptedException interruptedException) {
					Thread.currentThread().interrupt();
					throw new RuntimeException(interruptedException);
				}
				backoffTime *= 2; // exponential backoff
			} catch (JsonProcessingException e) {
				throw new RuntimeException(e);
			}
		}

		throw new RuntimeException("Exceeded max attempts for API request");

	}

}
