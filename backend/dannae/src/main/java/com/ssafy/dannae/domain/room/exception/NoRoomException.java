package com.ssafy.dannae.domain.room.exception;

public class NoRoomException extends RuntimeException {

	public NoRoomException() {
	}

	public NoRoomException(String message) {
		super(message);
	}

	public NoRoomException(String message, Throwable cause) {
		super(message, cause);
	}

	public NoRoomException(Throwable cause) {
		super(cause);
	}
	
}
