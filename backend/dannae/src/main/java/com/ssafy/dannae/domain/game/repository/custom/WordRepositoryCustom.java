package com.ssafy.dannae.domain.game.repository.custom;

import static com.ssafy.dannae.domain.game.entity.QWord.*;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

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
			.orderBy(com.querydsl.core.types.dsl.Expressions.numberTemplate(Double.class, "function('rand')").asc())
			.limit(10)
			.fetch();

		List<Word> difficulty2Words = queryFactory
			.selectFrom(word1)
			.where(word1.difficulty.eq(2))
			.orderBy(com.querydsl.core.types.dsl.Expressions.numberTemplate(Double.class, "function('rand')").asc())
			.limit(8)
			.fetch();

		List<Word> difficulty3Words = queryFactory
			.selectFrom(word1)
			.where(word1.difficulty.eq(3))
			.orderBy(com.querydsl.core.types.dsl.Expressions.numberTemplate(Double.class, "function('rand')").asc())
			.limit(7)
			.fetch();

		List<Word> difficulty4Words = queryFactory
			.selectFrom(word1)
			.where(word1.difficulty.eq(4))
			.orderBy(com.querydsl.core.types.dsl.Expressions.numberTemplate(Double.class, "function('rand')").asc())
			.limit(5)
			.fetch();

		// 모든 난이도별 결과를 합쳐서 반환
		List<Word> result = new ArrayList<>();
		result.addAll(difficulty1Words);
		result.addAll(difficulty2Words);
		result.addAll(difficulty3Words);
		result.addAll(difficulty4Words);

		return result;
	}
}
