package com.ssafy.dannae.domain.player.service.impl;

import com.ssafy.dannae.domain.player.entity.Player;
import com.ssafy.dannae.domain.player.entity.PlayerAuthorization;
import com.ssafy.dannae.domain.player.entity.PlayerStatus;
import com.ssafy.dannae.domain.player.repository.PlayerRepository;
import com.ssafy.dannae.domain.player.service.PlayerQueryService;
import com.ssafy.dannae.domain.player.service.dto.PlayerDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class PlayerQueryServiceImpl implements PlayerQueryService {
    private final PlayerRepository playerRepository;

    public PlayerDto createPlayer(PlayerDto dto){
        Player player = playerRepository.save(Player.builder()
                .roomId(dto.roomId())
                .score(dto.score())
                .status(PlayerStatus.valueOf(dto.status()))
                .authorization(PlayerAuthorization.valueOf(dto.authorization()))
                .nickname(dto.nickname())
                .image(dto.image())
                .build());

        return PlayerDto.builder()
                .playerId(player.getId())
                .build();
    }
}
