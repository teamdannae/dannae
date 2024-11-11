package com.ssafy.dannae.domain.game.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.dannae.domain.game.controller.Response.WordRes;
import com.ssafy.dannae.domain.game.service.GameQueryService;
import com.ssafy.dannae.global.template.response.BaseResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/game")
@RestController
public class GameController {

	private final GameQueryService gameQueryService;

	@GetMapping("/{word}")
	public ResponseEntity<BaseResponse<?>> readWordMeanings(@PathVariable String word) {

		WordRes res = WordRes.builder()
			.wordMeanings(gameQueryService.readWordMeanings(word))
			.build();

		return ResponseEntity.ok(BaseResponse.ofSuccess(res));

	}
}
