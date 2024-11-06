package com.ssafy.dannae.domain.player.service.impl;

import com.ssafy.dannae.domain.player.entity.Player;
import com.ssafy.dannae.domain.player.entity.PlayerAuthorization;
import com.ssafy.dannae.domain.player.entity.PlayerStatus;
import com.ssafy.dannae.domain.player.exception.NoPlayerException;
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
                .status(dto.status())  // 열거형으로 바로 설정
                .nickname(dto.nickname())
                .image(dto.image())
                .build());

        return PlayerDto.builder()
                .playerId(player.getId())
                .roomId(player.getRoomId())
                .score(player.getScore())
                .status(player.getStatus())
                .nickname(player.getNickname())
                .image(player.getImage())
                .build();
    }

    @Override
    public PlayerDto findPlayerById(Long playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new NoPlayerException("해당 플레이어가 존재하지 않습니다."));

        return PlayerDto.builder()
                .playerId(player.getId())
                .roomId(player.getRoomId())
                .score(player.getScore())
                .status(player.getStatus())
                .nickname(player.getNickname())
                .image(player.getImage())
                .build();
    }
}
