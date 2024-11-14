package com.ssafy.dannae.domain.room.exception;

public class TitleNumberOverflowException extends RuntimeException {

	public TitleNumberOverflowException() {
	}

	public TitleNumberOverflowException(String message) {
		super(message);
	}

	public TitleNumberOverflowException(String message, Throwable cause) {
		super(message, cause);
	}

	public TitleNumberOverflowException(Throwable cause) {
		super(cause);
	}
	
}
