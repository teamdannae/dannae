package com.ssafy.dannae.global.openai.exception;

public class OpenAITooManyRequestsException extends RuntimeException {

	public OpenAITooManyRequestsException() {
	}

	public OpenAITooManyRequestsException(String message) {
		super(message);
	}

	public OpenAITooManyRequestsException(String message, Throwable cause) {
		super(message, cause);
	}

	public OpenAITooManyRequestsException(Throwable cause) {
		super(cause);
	}
	
}
