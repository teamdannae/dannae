package com.ssafy.dannae.domain.game.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ssafy.dannae.domain.game.entity.Word;
import org.springframework.data.jpa.repository.Query;

public interface WordRepository extends JpaRepository<Word, Long> {

	Optional<List<Word>> findAllByInitialAndWord(String initial, String word);

	@Query(value = "SELECT * FROM word ORDER BY RANDOM() LIMIT 30", nativeQuery = true)
	List<Word> findRandomWords();

	Optional<Word> findByWord(String word);

}
