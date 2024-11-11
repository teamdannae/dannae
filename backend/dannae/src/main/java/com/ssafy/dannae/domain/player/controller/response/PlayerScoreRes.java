package com.ssafy.dannae.domain.player.controller.response;

import java.util.List;

import com.ssafy.dannae.domain.player.entity.Player;

import lombok.Builder;

@Builder
public record PlayerScoreRes(
	List<Player> playerList
) {
}
