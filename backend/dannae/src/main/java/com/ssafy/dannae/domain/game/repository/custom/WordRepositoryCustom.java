package com.ssafy.dannae.domain.game.repository.custom;

import static com.ssafy.dannae.domain.game.entity.QWord.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.ssafy.dannae.domain.game.entity.Word;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Repository
public class WordRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	public List<Word> findRandomWords() {
		List<Word> difficulty1Words = queryFactory
			.selectFrom(word1)
			.where(word1.difficulty.eq(1))
			.orderBy(Expressions.numberTemplate(Double.class, "RANDOM()").asc())
			.limit(10)
			.fetch();

		List<Word> difficulty2Words = queryFactory
			.selectFrom(word1)
			.where(word1.difficulty.eq(2))
			.orderBy(Expressions.numberTemplate(Double.class, "RANDOM()").asc())
			.limit(8)
			.fetch();

		List<Word> difficulty3Words = queryFactory
			.selectFrom(word1)
			.where(word1.difficulty.eq(3))
			.orderBy(Expressions.numberTemplate(Double.class, "RANDOM()").asc())
			.limit(7)
			.fetch();

		List<Word> difficulty4Words = queryFactory
			.selectFrom(word1)
			.where(word1.difficulty.eq(4))
			.orderBy(Expressions.numberTemplate(Double.class, "RANDOM()").asc())
			.limit(5)
			.fetch();

		List<Word> result = new ArrayList<>();
		result.addAll(difficulty1Words);
		result.addAll(difficulty2Words);
		result.addAll(difficulty3Words);
		result.addAll(difficulty4Words);

		Collections.shuffle(result);

		return result;
	}
}