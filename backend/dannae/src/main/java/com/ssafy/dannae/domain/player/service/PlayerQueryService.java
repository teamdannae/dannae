package com.ssafy.dannae.domain.player.service;

import com.ssafy.dannae.domain.player.entity.Player;
import com.ssafy.dannae.domain.player.service.dto.PlayerDto;

public interface PlayerQueryService {

    PlayerDto createPlayer(PlayerDto playerDto);

    PlayerDto findPlayerById(Long playerId);

}
