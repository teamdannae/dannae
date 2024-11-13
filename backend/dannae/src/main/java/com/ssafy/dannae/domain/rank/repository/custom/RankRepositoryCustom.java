package com.ssafy.dannae.domain.rank.repository.custom;

import static com.ssafy.dannae.domain.rank.entity.QRank.*;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.ssafy.dannae.domain.rank.entity.Rank;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Repository
public class RankRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	public List<Rank> findTop5ByMode(String mode) {
		return queryFactory
			.selectFrom(rank)
			.where(modeEq(mode))
			.orderBy(rank.score.desc())
			.limit(5)
			.fetch();
	}

	// mode에 따른 동적 조건 생성
	private BooleanExpression modeEq(String mode) {
		return mode != null ? rank.mode.eq(mode) : null;
	}

}