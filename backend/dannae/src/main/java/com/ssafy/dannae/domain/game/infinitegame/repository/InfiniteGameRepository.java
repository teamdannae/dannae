package com.ssafy.dannae.domain.game.infinitegame.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ssafy.dannae.domain.game.infinitegame.entity.InfiniteGame;

public interface InfiniteGameRepository extends JpaRepository<InfiniteGame, Long> {

}
