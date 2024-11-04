package com.ssafy.dannae.domain.player.service.impl;

import com.ssafy.dannae.domain.player.entity.Player;
import com.ssafy.dannae.domain.player.repository.PlayerRepository;
import com.ssafy.dannae.domain.player.service.PlayerCommandService;
import com.ssafy.dannae.domain.room.exception.NoRoomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class PlayerCommandServiceImpl implements PlayerCommandService {

    private final PlayerRepository playerRepository;

    @Override
    public void updateAuthorization(Long playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new NoRoomException("player not found"));
        player.updateAuthorization();
    }

}
