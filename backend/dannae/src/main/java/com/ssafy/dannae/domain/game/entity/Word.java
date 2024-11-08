package com.ssafy.dannae.domain.game.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "word",
	indexes = @Index(name = "idx_initial", columnList = "initial"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Word {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "word_id")
	private Long id;

	@Column(name = "word", nullable = false)
	private String word;

	@Column(name = "meaning", nullable = false)
	private String meaning;

	@Column(name = "difficulty")
	private Integer difficulty;

	@Column(name = "initial")
	private String initial;

	@Column(name = "game_count")
	private Long gameCount;

	/**
	 * 단어와 뜻만 지닌 data를 넣을 용도의 생성자
	 */
	@Builder
	public Word(String word, String meaning) {
		this(word, meaning, null, null, null);
	}

	/**
	 * 초성게임에서 사용하는 data를 넣을 용도의 생성자
	 */
	@Builder
	public Word(String word, String meaning, String initial) {
		this(word, meaning, null, initial, 0L);
	}

	public Word(String word, String meaning, Integer difficulty, String initial, Long gameCount) {
		this.word = word;
		this.meaning = meaning;
		this.difficulty = difficulty;
		this.initial = initial;
		this.gameCount = gameCount;
	}

	public void updateDifficulty(Integer difficulty){
		this.difficulty = difficulty;
	}

	public void updateGameCount(){
		this.gameCount++;
	}

}
