package com.ssafy.dannae.domain.player.controller.response;

import java.util.List;

import com.ssafy.dannae.domain.player.service.dto.PlayerDto;

import lombok.Builder;

@Builder
public record PlayerScoreRes(
	List<PlayerDto> playerList
) {
}
