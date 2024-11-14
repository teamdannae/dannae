package com.ssafy.dannae.domain.player.repository;

import com.ssafy.dannae.domain.player.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepository extends JpaRepository<Player, Long> {

}
