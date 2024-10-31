package com.ssafy.dannae.global.openai.exception;

public class OpenAIRequestProcessingException extends RuntimeException {

	public OpenAIRequestProcessingException() {
	}

	public OpenAIRequestProcessingException(String message) {
		super(message);
	}

	public OpenAIRequestProcessingException(String message, Throwable cause) {
		super(message, cause);
	}

	public OpenAIRequestProcessingException(Throwable cause) {
		super(cause);
	}
	
}
