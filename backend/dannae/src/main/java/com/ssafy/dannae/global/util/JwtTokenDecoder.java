package com.ssafy.dannae.global.util;

import org.springframework.stereotype.Component;

import com.ssafy.dannae.domain.player.exception.TokenException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class JwtTokenDecoder {

	private final JwtTokenProvider jwtTokenProvider;

	public Long getPlayerId(String token) {

		String jwtToken = token.startsWith("Bearer ") ? token.substring(7) : token;

		if (!jwtTokenProvider.validateToken(jwtToken)) {
			throw new TokenException("유효하지 않거나 만료된 토큰입니다.");
		}

		String id = jwtTokenProvider.getPlayerIdFromToken(jwtToken);
		Long playerId = Long.parseLong(id);

		return playerId;
	}
}
