package com.ssafy.dannae.domain.rank.service;

import java.util.List;

import com.ssafy.dannae.domain.rank.entity.Rank;

public interface RankQueryService {

    List<Rank> readRanksSortedByScoreDesc(String mode);

}
