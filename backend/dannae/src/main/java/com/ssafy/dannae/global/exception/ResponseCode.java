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
	ROOM_NOT_AVAILABLE_EXCEPTION("3001", "방이 없습니다.");

	private String code;
	private String message;

}
