package com.ssafy.dannae.domain.game.controller.Response;

import java.util.List;

import lombok.Builder;

@Builder
public record WordRes(
	List<String> wordMeanings
) {
}
