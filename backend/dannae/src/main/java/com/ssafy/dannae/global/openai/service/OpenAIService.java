package com.ssafy.dannae.global.openai.service;

import com.ssafy.dannae.global.openai.service.dto.PromptDto;
import com.ssafy.dannae.global.openai.service.dto.SentenceDto;
import com.ssafy.dannae.global.openai.service.dto.WordResultDto;

public interface OpenAIService {

	String prompt(PromptDto promptDto);

	WordResultDto wordResult(SentenceDto sentenceDto);

	String filterMessage(String message);

}
