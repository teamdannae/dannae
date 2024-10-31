package com.ssafy.dannae.global.openai.exception;

public class OpenAIResponseProcessingException extends RuntimeException {

	public OpenAIResponseProcessingException() {
	}

	public OpenAIResponseProcessingException(String message) {
		super(message);
	}

	public OpenAIResponseProcessingException(String message, Throwable cause) {
		super(message, cause);
	}

	public OpenAIResponseProcessingException(Throwable cause) {
		super(cause);
	}
	
}
