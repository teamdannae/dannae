package com.ssafy.dannae.domain.player.service;

import com.ssafy.dannae.domain.player.service.dto.PlayerDto;

public interface PlayerCommandService {
    void updateAuthorization(Long playerId);
}
