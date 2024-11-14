package com.ssafy.dannae.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseCode {

	// 2000 - 성공
	OK("2000", "성공"),

	// 3000 - 게임 방
	TITLE_NUMBER_OVERFLOW_EXCEPTION("3000", "제목이 허용된 길이를 초과하였습니다"),
	ROOM_NOT_AVAILABLE_EXCEPTION("3001", "방이 없습니다."),
	ALREADY_IN_ROOM_EXCEPTION("3002", "이미 방에 들어가있습니다."),

	// 4000 - OpenAI 관련 에러
	OPENAI_REQUEST_PROCESSING_ERROR("4000", "OpenAI 요청 처리 중 오류가 발생했습니다"),
	OPENAI_RESPONSE_PROCESSING_ERROR("4001", "OpenAI 응답 처리 중 오류가 발생했습니다"),
	OPENAI_AUTHENTICATION_ERROR("4002", "OpenAI 인증에 실패했습니다"),
	OPENAI_TOO_MANY_REQUESTS_ERROR("4003", "OpenAI API 요청 한도를 초과했습니다"),
	OPENAI_TIMEOUT_ERROR("4004", "OpenAI API 요청 시간이 초과되었습니다"),
	OPENAI_UNEXPECTED_ERROR("4005", "OpenAI 서비스 처리 중 예상치 못한 오류가 발생했습니다"),

	// 5000 - jwt 토큰 에러
	TOKEN_ERROR("5000", "JWT 토큰이 유효하지않습니다.");

	private String code;
	private String message;
}