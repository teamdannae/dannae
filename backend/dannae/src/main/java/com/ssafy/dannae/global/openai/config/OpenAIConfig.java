package com.ssafy.dannae.global.openai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Configuration
public class OpenAIConfig {

	@Bean
	public RestClient openAiRestClient() {
		return RestClient.builder()
			.baseUrl("https://api.openai.com/v1")
			.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.build();
	}
}