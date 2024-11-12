package com.ssafy.dannae.domain.rank.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.dannae.domain.rank.entity.Rank;
import com.ssafy.dannae.domain.rank.repository.RankRepository;
import com.ssafy.dannae.domain.rank.service.RankCommandService;
import com.ssafy.dannae.domain.rank.service.dto.RankDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class RankCommandServiceImpl implements RankCommandService {

    private final RankRepository rankRepository;

    @Override
    public RankDto createRank(RankDto dto) {
        Rank rank = rankRepository.save(Rank.builder()
                .score(dto.score())
                .mode(dto.mode())
                .nickname(dto.nickname())
                .image(dto.image())
                .build());

        return RankDto.builder()
                .id(rank.getId())
                .score(rank.getScore())
                .mode(rank.getMode())
                .nickname(rank.getNickname())
                .image(rank.getImage())
                .build();
    }

    @Override
    public void deleteRank(Long id) {
        rankRepository.deleteById(id);
    }

}
