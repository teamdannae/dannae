package com.ssafy.dannae.domain.rank.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
public class RankCommandServiceImpl implements RankCommandService {

    private final RankRepository rankRepository;
    private final PlayerQueryService playerQueryService;

    @Override
    public void deleteRank(Long id) {
        rankRepository.deleteById(id);
    }

    @Override
    public void updateRank(String mode, PlayerIdListDto playerIdListDto) {

        List<Rank> rankList = rankRepository.findAllByMode(mode);

        List<Long> playerIdList = playerIdListDto.playerIdList();

        for(Long playerId : playerIdList) {
            PlayerDto player = playerQueryService.findPlayerById(playerId);
            rankList.sort((r1, r2) -> Long.compare(r2.getScore(), r1.getScore()));
            if (rankList.size() < 5 || player.score() > rankList.get(4).getScore()) {
                if (rankList.size() >= 5 && player.score() == rankList.get(4).getScore()) {
                    continue;
                }
                rankRepository.save(
                    Rank.builder()
                        .nickname(player.nickname())
                        .mode(mode)
                        .score(player.score())
                        .image(player.image())
                        .build()
                );
            }
        }


    }

}
