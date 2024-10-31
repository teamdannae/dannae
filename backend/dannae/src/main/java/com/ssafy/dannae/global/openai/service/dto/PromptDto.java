package com.ssafy.dannae.global.openai.service.dto;

import java.util.List;
import java.util.Map;

import lombok.Builder;

@Builder
public record PromptDto(
	String model,
	List<Map<String, String>>messages,
	float temperature,
	int max_tokens
) {
}
