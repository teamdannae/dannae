package com.ssafy.dannae.domain.player.controller.request;

import java.util.List;

import lombok.Builder;

@Builder
public record PlayerIdReq(
	List<Long> playerIdList
) {
}
