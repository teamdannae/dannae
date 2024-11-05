package com.ssafy.dannae.domain.game.exception;

public class NoWordException extends RuntimeException {

	public NoWordException() {}

	public NoWordException(String message) {
		super(message);
	}

	public NoWordException(String message, Throwable cause) {
		super(message, cause);
	}

	public NoWordException(Throwable cause) {
		super(cause);
	}

}
