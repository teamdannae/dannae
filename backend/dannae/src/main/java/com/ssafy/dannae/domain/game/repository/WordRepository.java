package com.ssafy.dannae.domain.game.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ssafy.dannae.domain.game.entity.Word;

public interface WordRepository extends JpaRepository<Word, Long> {

	Optional<Word> findByInitialAndWord(String initial, String word);

}
