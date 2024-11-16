package com.ssafy.dannae.domain.player.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.dannae.domain.player.entity.Player;
import com.ssafy.dannae.domain.player.entity.PlayerStatus;
import com.ssafy.dannae.domain.player.repository.PlayerRepository;
import com.ssafy.dannae.domain.player.service.PlayerCommandService;
import com.ssafy.dannae.domain.room.exception.NoRoomException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class PlayerCommandServiceImpl implements PlayerCommandService {

    private final PlayerRepository playerRepository;

    @Override
    public void updateStatus(Long playerId, PlayerStatus status) {
        Player player = verifyPlayer(playerId);
        player.updateStatus(status);
    }

    @Override
    public void resetScore(Long playerId) {
        Player player = verifyPlayer(playerId);
        player.resetScore();
        playerRepository.save(player);
    }

    @Override
    public void updateScore(Long playerId, int score) {
        Player player = verifyPlayer(playerId);
        player.updateScore(score);
        playerRepository.save(player);
    }

    private Player verifyPlayer(Long playerId) {
        return playerRepository.findById(playerId)
            .orElseThrow(() -> new NoRoomException("player not found"));
    }

}
