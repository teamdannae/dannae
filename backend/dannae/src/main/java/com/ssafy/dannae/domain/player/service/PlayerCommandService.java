package com.ssafy.dannae.domain.player.service;

import com.ssafy.dannae.domain.player.entity.PlayerStatus;

public interface PlayerCommandService {

    void updateStatus(Long playerId, PlayerStatus status);

    void resetScore(Long playerId);

    void updateScore(Long playerId, int score);

}
