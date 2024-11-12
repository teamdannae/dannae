package com.ssafy.dannae.domain.rank.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.dannae.domain.rank.entity.Rank;
import com.ssafy.dannae.domain.rank.repository.custom.RankRepositoryCustom;
import com.ssafy.dannae.domain.rank.service.RankQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class RankQueryServiceImpl implements RankQueryService {

    private final RankRepositoryCustom rankRepositoryCustom;

    @Override
    public List<Rank> readRanksSortedByScoreDesc(String mode) {
        return rankRepositoryCustom.findTop5ByMode(mode);
    }

}
