package com.ssafy.dannae.domain.rank.service.impl;

import com.ssafy.dannae.domain.rank.repository.RankRepository;
import com.ssafy.dannae.domain.rank.service.RankQueryService;
import com.ssafy.dannae.domain.rank.service.dto.RankDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class RankQueryServiceImpl implements RankQueryService {

    private final RankRepository rankRepository;

    @Override
    public List<RankDto> getRanksSortedByScoreDesc(String mode) {
        return rankRepository.findAllByModeOrderByScoreDesc(mode);
    }
}
