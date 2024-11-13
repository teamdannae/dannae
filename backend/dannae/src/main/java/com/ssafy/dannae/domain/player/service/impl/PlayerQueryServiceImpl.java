package com.ssafy.dannae.domain.player.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.dannae.domain.player.entity.Player;
import com.ssafy.dannae.domain.player.exception.NoPlayerException;
import com.ssafy.dannae.domain.player.repository.PlayerRepository;
import com.ssafy.dannae.domain.player.service.PlayerQueryService;
import com.ssafy.dannae.domain.player.service.dto.PlayerDto;
import com.ssafy.dannae.domain.player.service.dto.PlayerIdListDto;
import com.ssafy.dannae.domain.rank.entity.Rank;
import com.ssafy.dannae.domain.rank.repository.RankRepository;
import com.ssafy.dannae.domain.rank.service.RankCommandService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class PlayerQueryServiceImpl implements PlayerQueryService {

    private final PlayerRepository playerRepository;
    private final RankRepository rankRepository;
    private final RankCommandService rankCommandService;

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
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new NoPlayerException("해당 플레이어가 존재하지 않습니다."));

        return PlayerDto.builder()
                .playerId(player.getId())
                .score(player.getScore())
                .status(player.getStatus())
                .nickname(player.getNickname())
                .image(player.getImage())
                .build();
    }

    @Override
    public List<Player> readPlayerTotalScore(PlayerIdListDto playerIdListDto, String mode){
        List<Player> playerList = new ArrayList<>();
        List<Long> playerIdList = playerIdListDto.playerIdList();
        for(Long playerId : playerIdList){
            Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new NoPlayerException("No player find by Id."+ playerId));
            List<Rank> rankList = rankRepository.findAllByMode(mode);
            rankList.sort((r1, r2) -> Long.compare(r2.getScore(), r1.getScore()));
            if (rankList.size() < 5 || player.getScore() > rankList.get(4).getScore()) {
                if (rankList.size() >= 5 && player.getScore() == rankList.get(4).getScore()) {
                    continue;
                }
                rankRepository.save(
                    Rank.builder()
                        .nickname(player.getNickname())
                        .mode(mode)
                        .score(player.getScore())
                        .image(player.getImage())
                        .build()
                );
            }
            playerList.add(player);
        }
        playerList.sort((p1, p2) -> Long.compare(p2.getScore(), p1.getScore()));

        return playerList;
    }

}
