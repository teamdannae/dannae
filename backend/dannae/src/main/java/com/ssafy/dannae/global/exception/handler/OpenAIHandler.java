package com.ssafy.dannae.global.exception.handler;

import static com.ssafy.dannae.global.exception.ResponseCode.*;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ssafy.dannae.global.openai.exception.OpenAIAuthenticationException;
import com.ssafy.dannae.global.openai.exception.OpenAIRequestProcessingException;
import com.ssafy.dannae.global.openai.exception.OpenAIResponseProcessingException;
import com.ssafy.dannae.global.openai.exception.OpenAIServiceException;
import com.ssafy.dannae.global.openai.exception.OpenAITimeoutException;
import com.ssafy.dannae.global.openai.exception.OpenAITooManyRequestsException;
import com.ssafy.dannae.global.template.response.BaseResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
public class OpenAIHandler {

	@ExceptionHandler(OpenAIRequestProcessingException.class)
	public ResponseEntity<BaseResponse<Object>> handleRequestProcessingException(OpenAIRequestProcessingException e) {
		log.error("OpenAI 요청 처리 오류: {}", e.getMessage(), e);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(BaseResponse.ofFail(OPENAI_REQUEST_PROCESSING_ERROR));
	}

	@ExceptionHandler(OpenAIResponseProcessingException.class)
	public ResponseEntity<BaseResponse<Object>> handleResponseProcessingException(OpenAIResponseProcessingException e) {
		log.error("OpenAI 응답 처리 오류: {}", e.getMessage(), e);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(BaseResponse.ofFail(OPENAI_RESPONSE_PROCESSING_ERROR));
	}

	@ExceptionHandler(OpenAIAuthenticationException.class)
	public ResponseEntity<BaseResponse<Object>> handleAuthenticationException(OpenAIAuthenticationException e) {
		log.error("OpenAI 인증 오류: {}", e.getMessage(), e);
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
			.body(BaseResponse.ofFail(OPENAI_AUTHENTICATION_ERROR));
	}

	@ExceptionHandler(OpenAITooManyRequestsException.class)
	public ResponseEntity<BaseResponse<Object>> handleTooManyRequestsException(OpenAITooManyRequestsException e) {
		log.error("OpenAI 요청 한도 초과: {}", e.getMessage(), e);
		return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
			.body(BaseResponse.ofFail(OPENAI_TOO_MANY_REQUESTS_ERROR));
	}

	@ExceptionHandler(OpenAITimeoutException.class)
	public ResponseEntity<BaseResponse<Object>> handleTimeoutException(OpenAITimeoutException e) {
		log.error("OpenAI 타임아웃: {}", e.getMessage(), e);
		return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
			.body(BaseResponse.ofFail(OPENAI_TIMEOUT_ERROR));
	}

	@ExceptionHandler(OpenAIServiceException.class)
	public ResponseEntity<BaseResponse<Object>> handleServiceException(OpenAIServiceException e) {
		log.error("OpenAI 서비스 오류: {}", e.getMessage(), e);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(BaseResponse.ofFail(OPENAI_UNEXPECTED_ERROR));
	}
}