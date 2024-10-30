package com.ssafy.dannae.global.exception.handler;

import static com.ssafy.dannae.global.exception.ResponseCode.*;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ssafy.dannae.domain.room.exception.TitleNumberOverflowException;
import com.ssafy.dannae.global.template.response.BaseResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
public class RoomHandler {

	@ExceptionHandler(TitleNumberOverflowException.class)
	public ResponseEntity<BaseResponse<Object>> titleNumberOverflowException(TitleNumberOverflowException e) {
		log.info(e.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(BaseResponse.ofFail(TITLE_NUMBER_OVERFLOW_EXCEPTION));
	}
}
