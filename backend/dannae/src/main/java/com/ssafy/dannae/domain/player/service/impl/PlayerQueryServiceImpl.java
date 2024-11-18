package com.ssafy.dannae.domain.player.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.dannae.domain.player.entity.Player;
import com.ssafy.dannae.domain.player.entity.PlayerStatus;
import com.ssafy.dannae.domain.player.exception.AlreadyEnteredException;
import com.ssafy.dannae.domain.player.exception.NoPlayerException;
import com.ssafy.dannae.domain.player.repository.PlayerRepository;
import com.ssafy.dannae.domain.player.service.PlayerQueryService;
import com.ssafy.dannae.domain.player.service.dto.PlayerDto;
import com.ssafy.dannae.domain.player.service.dto.PlayerIdListDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class PlayerQueryServiceImpl implements PlayerQueryService {

    private final PlayerRepository playerRepository;

    @Override
    public PlayerDto createPlayer(PlayerDto dto){
        Player player = playerRepository.save(Player.builder()
                .score(dto.score())
                .status(dto.status())
                .nickname(dto.nickname())
                .image(dto.image())
                .build());

        return PlayerDto.builder()
                .playerId(player.getId())
                .score(player.getScore())
                .status(player.getStatus())
                .nickname(player.getNickname())
                .image(player.getImage())
                .build();
    }

    @Override
    public PlayerDto findPlayerById(Long playerId) {
        Player player = verifyPlayer(playerId);

        return PlayerDto.builder()
                .playerId(player.getId())
                .score(player.getScore())
                .status(player.getStatus())
                .nickname(player.getNickname())
                .image(player.getImage())
                .build();
    }

    @Override
    public List<PlayerDto> readPlayerTotalScore(PlayerIdListDto playerIdListDto, String mode){
        List<PlayerDto> playerList = new ArrayList<>();
        List<Long> playerIdList = playerIdListDto.playerIdList();
        for(Long playerId : playerIdList){
            Player player = verifyPlayer(playerId);
            playerList.add(PlayerDto.builder()
                    .playerId(player.getId())
                    .score(player.getScore())
                    .status(player.getStatus())
                    .nickname(player.getNickname())
                    .image(player.getImage())
                    .build());

            player.updateStatus(PlayerStatus.none);
        }
        playerList.sort((p1, p2) -> Long.compare(p2.score(), p1.score()));

        return playerList;
    }

    @Override
    public boolean canEnterRoom(long playerId) {
        Player player = verifyPlayer(playerId);
        if (player.getStatus() == PlayerStatus.none) {
            return true;
        }else{
            throw new AlreadyEnteredException("이미 방에 들어가 있습니다");
        }
    }

    private Player verifyPlayer(Long playerId) {
        return playerRepository.findById(playerId)
            .orElseThrow(() -> new NoPlayerException("player not found"));
    }

}
