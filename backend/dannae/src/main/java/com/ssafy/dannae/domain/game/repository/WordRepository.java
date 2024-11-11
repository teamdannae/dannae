package com.ssafy.dannae.domain.game.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.ssafy.dannae.domain.game.entity.Word;

import io.lettuce.core.dynamic.annotation.Param;

public interface WordRepository extends JpaRepository<Word, Long> {

	Optional<List<Word>> findAllByInitialAndWord(String initial, String word);

	@Query(value = "SELECT * FROM word ORDER BY RANDOM() LIMIT 30", nativeQuery = true)
	List<Word> findRandomWords();

	Optional<List<Word>> findByWord(String word);

	@Query("SELECT w FROM Word w WHERE w.word = :word ORDER BY w.id ASC")
	Optional<Word> findFirstByWord(@Param("word") String word);

}
