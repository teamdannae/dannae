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
        Player player = playerRepository.findById(playerId)
            .orElseThrow(() -> new NoRoomException("player not found"));
        player.updateStatus(status);
    }

    @Override
    public void resetScore(Long playerId) {
        Player player = playerRepository.findById(playerId)
            .orElseThrow(() -> new NoRoomException("player not found"));
        player.resetScore();
        playerRepository.save(player);
    }

}
