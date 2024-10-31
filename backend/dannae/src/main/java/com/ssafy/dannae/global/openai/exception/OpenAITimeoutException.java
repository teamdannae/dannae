package com.ssafy.dannae.global.openai.exception;

public class OpenAITimeoutException extends RuntimeException {

	public OpenAITimeoutException() {
	}

	public OpenAITimeoutException(String message) {
		super(message);
	}

	public OpenAITimeoutException(String message, Throwable cause) {
		super(message, cause);
	}

	public OpenAITimeoutException(Throwable cause) {
		super(cause);
	}
	
}
