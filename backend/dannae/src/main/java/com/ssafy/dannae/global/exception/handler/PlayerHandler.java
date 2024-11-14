package com.ssafy.dannae.global.exception.handler;

import com.ssafy.dannae.domain.player.exception.TokenException;
import com.ssafy.dannae.global.template.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.ssafy.dannae.global.exception.ResponseCode.TITLE_NUMBER_OVERFLOW_EXCEPTION;
import static com.ssafy.dannae.global.exception.ResponseCode.TOKEN_ERROR;

@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
public class PlayerHandler {

    @ExceptionHandler(TokenException.class)
    public ResponseEntity<BaseResponse<Object>> tokenException(final TokenException e) {
        log.info(e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.ofFail(TOKEN_ERROR));
    }
}
