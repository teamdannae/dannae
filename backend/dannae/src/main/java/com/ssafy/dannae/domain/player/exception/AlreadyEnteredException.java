package com.ssafy.dannae.domain.player.exception;

public class AlreadyEnteredException extends RuntimeException {
    public AlreadyEnteredException(String message) {
        super(message);
    }
}
