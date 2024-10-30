package com.ssafy.dannae.global.template.response;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fooding.api.core.exception.ResponseCode;

import lombok.Builder;
import lombok.Getter;

@Getter
public class BaseResponse<T> {

	private static final String SUCCESS = "SUCCESS";
	private static final String FAIL = "FAIL";

	private final String code;
	@JsonProperty(value = "isSuccess")
	private final boolean success;
	private final String message;
	private final T data;

	@Builder
	private BaseResponse(String code, String message, boolean success, T data) {
		this.code = code;
		this.success = success;
		this.message = message;
		this.data = data;
	}

	/**
	 * 반환 데이터가 없는 성공 메시지 템플릿
	 * @return BaseResponse 객체만 반한
	 * @param <T> 반환 데이터의 타입을 나타내는 제네릭 타입 파라미터
	 */
	public static <T> BaseResponse<T> ofSuccess() {
		return BaseResponse.<T>builder()
			.code(ResponseCode.OK.getCode())
			.message(ResponseCode.OK.getMessage())
			.success(true)
			.data(null)
			.build();
	}

	/**
	 * 반환 데이터가 있는 성공 메시지 템플릿
	 * @param data 반환 데이터
	 * @return BaseResponse 객체만 반한
	 * @param <T> 반환 데이터의 타입을 나타내는 제네릭 타입 파라미터
	 */
	public static <T> BaseResponse<T> ofSuccess(T data) {
		return BaseResponse.<T>builder()
			.code(ResponseCode.OK.getCode())
			.message(ResponseCode.OK.getMessage())
			.success(true)
			.data(data)
			.build();
	}

	/**
	 * 예외를 반환하는 실패 메시지 템플릿
	 * @param responseCode 상태코드 enum 클래스
	 * @return 예외를 반환하는 실패 메시지 템플릿
	 * @param <T> 반환 데이터의 타입을 나타내는 제네릭 타입 파라미터
	 * @see ResponseCode
	 */
	public static <T> BaseResponse<T> ofFail(ResponseCode responseCode) {
		return BaseResponse.<T>builder()
			.code(responseCode.getCode())
			.message(responseCode.getMessage())
			.success(false)
			.data(null)
			.build();
	}

}
