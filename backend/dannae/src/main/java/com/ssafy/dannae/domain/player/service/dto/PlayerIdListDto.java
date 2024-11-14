package com.ssafy.dannae.domain.player.service.dto;

import java.util.List;

import lombok.Builder;

@Builder
public record PlayerIdListDto(
	List<Long> playerIdList
) {
}
