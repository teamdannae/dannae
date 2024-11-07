package com.ssafy.dannae.domain.game.infinitegame.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.dannae.domain.game.infinitegame.controller.request.InfiniteGameReq;
import com.ssafy.dannae.domain.game.infinitegame.controller.response.InfiniteGameCreateRes;
import com.ssafy.dannae.domain.game.infinitegame.controller.response.InfiniteGameRes;
import com.ssafy.dannae.domain.game.infinitegame.service.InfiniteGameCommandService;
import com.ssafy.dannae.domain.game.infinitegame.service.dto.InfiniteGameDto;
import com.ssafy.dannae.global.template.response.BaseResponse;
import com.ssafy.dannae.global.util.JwtTokenDecoder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/infinitegames")
@RestController
public class InfiniteGameController {

	private final InfiniteGameCommandService infiniteGameCommandService;
	private final JwtTokenDecoder jwtTokenDecoder;

	/**
	 * 초성을 제시해주는 method
	 * @return initial(초성), 게임번호
	 */
	@PostMapping("/{room-id}")
	public ResponseEntity<BaseResponse<?>> createInitial(@PathVariable("room-id") Long roomId) {

		InfiniteGameDto infiniteGameDto = infiniteGameCommandService.createInitial(InfiniteGameDto.builder()
			.roomId(roomId)
			.build());

		InfiniteGameCreateRes infiniteGameCreateRes = InfiniteGameCreateRes.builder()
			.id(infiniteGameDto.gameId())
			.initial(infiniteGameDto.initial())
			.build();

		return ResponseEntity.ok(BaseResponse.ofSuccess(infiniteGameCreateRes));
	}

	/**
	 * 단어가 제시된 초성에 맞는 사전에 존재하는 단어인지 확인하는 method
	 * @param req : 단어
	 * @return 단어, 단어 뜻, 난이도
	 */
	@PostMapping("/{room-id}/check")
	public ResponseEntity<BaseResponse<?>> updateWord(@RequestHeader(HttpHeaders.AUTHORIZATION) String token, @PathVariable("room-id") Long roomId, @RequestBody InfiniteGameReq req){

		Long playerId = jwtTokenDecoder.getPlayerId(token);

		InfiniteGameDto dto = infiniteGameCommandService.updateWord(InfiniteGameDto.builder()
			.word(req.word())
			.gameId(req.gameId())
			.roomId(roomId)
			.initial(req.initial())
			.playerId(playerId)
			.build());

		InfiniteGameRes res = InfiniteGameRes.builder()
			.correct(dto.correct())
			.word(dto.word())
			.meaning(dto.meaning())
			.difficulty(dto.difficulty())
			.build();

		return ResponseEntity.ok(BaseResponse.ofSuccess(res));
	}
}
