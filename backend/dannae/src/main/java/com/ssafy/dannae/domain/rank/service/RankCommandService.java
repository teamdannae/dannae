package com.ssafy.dannae.domain.rank.service;

import com.ssafy.dannae.domain.player.service.dto.PlayerIdListDto;

public interface RankCommandService {

    void deleteRank(Long id);

    void updateRank(String mode, PlayerIdListDto playerIdListDto);

}
