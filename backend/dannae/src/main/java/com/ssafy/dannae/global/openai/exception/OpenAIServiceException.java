package com.ssafy.dannae.global.openai.exception;

public class OpenAIServiceException extends RuntimeException {

	public OpenAIServiceException() {
	}

	public OpenAIServiceException(String message) {
		super(message);
	}

	public OpenAIServiceException(String message, Throwable cause) {
		super(message, cause);
	}

	public OpenAIServiceException(Throwable cause) {
		super(cause);
	}
	
}
