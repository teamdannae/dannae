package com.ssafy.dannae.domain.rank.repository;

import com.ssafy.dannae.domain.rank.entity.Rank;
import com.ssafy.dannae.domain.rank.service.dto.RankDto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RankRepository extends JpaRepository<Rank, Long> {

    List<RankDto> findAllByModeOrderByScoreDesc(String mode);
}
