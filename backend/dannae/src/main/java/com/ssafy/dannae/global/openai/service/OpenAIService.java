package com.ssafy.dannae.global.openai.service;

import com.ssafy.dannae.global.openai.service.dto.PromptDto;

public interface OpenAIService {

	String prompt(PromptDto promptDto);

}
