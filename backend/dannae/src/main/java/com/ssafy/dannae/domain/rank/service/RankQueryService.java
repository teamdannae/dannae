package com.ssafy.dannae.domain.rank.service;

import com.ssafy.dannae.domain.rank.service.dto.RankDto;

import java.util.List;

public interface RankQueryService {
    List<RankDto> getRanksSortedByScoreDesc(String mode);
}
