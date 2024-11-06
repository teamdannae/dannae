package com.ssafy.dannae.domain.player.service;

import com.ssafy.dannae.domain.player.entity.PlayerStatus;
import com.ssafy.dannae.domain.player.service.dto.PlayerDto;

public interface PlayerCommandService {
    void updateStatus(Long playerId, PlayerStatus status);
}
