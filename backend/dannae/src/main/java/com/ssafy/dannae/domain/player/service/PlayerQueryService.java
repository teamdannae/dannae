package com.ssafy.dannae.domain.player.service;

import java.util.List;

import com.ssafy.dannae.domain.player.service.dto.PlayerDto;
import com.ssafy.dannae.domain.player.service.dto.PlayerIdListDto;

public interface PlayerQueryService {

    PlayerDto createPlayer(PlayerDto playerDto);

    PlayerDto findPlayerById(Long playerId);

    List<PlayerDto> readPlayerTotalScore(PlayerIdListDto playerIdListDto, String mode);

}
