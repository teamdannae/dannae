package com.ssafy.dannae.domain.rank.service;

import com.ssafy.dannae.domain.rank.service.dto.RankDto;


public interface RankCommandService {
    RankDto createRank(RankDto rankDto);

    void deleteRank(Long id);


}
