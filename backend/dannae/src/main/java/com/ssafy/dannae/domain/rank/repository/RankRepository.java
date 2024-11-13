package com.ssafy.dannae.domain.rank.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ssafy.dannae.domain.rank.entity.Rank;

public interface RankRepository extends JpaRepository<Rank, Long> {

    List<Rank> findAllByMode(String mode);

}
