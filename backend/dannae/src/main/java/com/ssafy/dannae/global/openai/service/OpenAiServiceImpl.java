package com.ssafy.dannae.global.openai.service;

import java.util.List;
import java.util.Map;

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
import com.ssafy.dannae.global.openai.exception.OpenAIAuthenticationException;
import com.ssafy.dannae.global.openai.exception.OpenAIRequestProcessingException;
import com.ssafy.dannae.global.openai.exception.OpenAIResponseProcessingException;
import com.ssafy.dannae.global.openai.exception.OpenAIServiceException;
import com.ssafy.dannae.global.openai.exception.OpenAITimeoutException;
import com.ssafy.dannae.global.openai.exception.OpenAITooManyRequestsException;
import com.ssafy.dannae.global.openai.service.dto.PromptDto;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Transactional
@Service
class OpenAiServiceImpl implements OpenAIService {

	private final ObjectMapper objectMapper;
	private final RestClient restClient;

	@Value("${openai.model}")
	private String model;

	@Value("${openai.api.key}")
	private String apiKey;

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

			return executeWithRetry(() -> sendRequest(requestBody));
		} catch (JsonProcessingException e) {
			throw new OpenAIRequestProcessingException("Failed to process request body", e);
		}
	}

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

	private String executeWithRetry(RetryableOperation operation) {
		int attempts = 0;
		int maxAttempts = 5;
		long backoffTime = 1000;

		while (attempts < maxAttempts) {
			try {
				return operation.execute();
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

	@FunctionalInterface
	private interface RetryableOperation {
		String execute() throws Exception;
	}
}