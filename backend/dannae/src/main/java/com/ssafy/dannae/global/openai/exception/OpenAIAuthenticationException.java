package com.ssafy.dannae.global.openai.exception;

public class OpenAIAuthenticationException extends RuntimeException {

	public OpenAIAuthenticationException() {
	}

	public OpenAIAuthenticationException(String message) {
		super(message);
	}

	public OpenAIAuthenticationException(String message, Throwable cause) {
		super(message, cause);
	}

	public OpenAIAuthenticationException(Throwable cause) {
		super(cause);
	}
	
}
