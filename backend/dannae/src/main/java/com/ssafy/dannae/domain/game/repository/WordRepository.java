package com.ssafy.dannae.domain.game.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ssafy.dannae.domain.game.entity.Word;

public interface WordRepository extends JpaRepository<Word, Long> {

	Optional<List<Word>> findAllByInitialAndWord(String initial, String word);

	Optional<List<Word>> findByWord(String word);

	Optional<Word> findFirstByWord(String word);

}
