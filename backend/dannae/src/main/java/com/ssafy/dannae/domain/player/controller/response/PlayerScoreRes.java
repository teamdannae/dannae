package com.ssafy.dannae.domain.player.controller.response;

import java.util.List;

import lombok.Builder;

@Builder
public record PlayerScoreRes(
	List<Long> scoreList,
	List<String> nicknameList
) {
}
