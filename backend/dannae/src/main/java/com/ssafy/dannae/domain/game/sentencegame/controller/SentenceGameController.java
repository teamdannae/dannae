package com.ssafy.dannae.domain.game.sentencegame.controller;


import com.ssafy.dannae.domain.game.infinitegame.service.dto.InfiniteGameDto;
import com.ssafy.dannae.domain.game.sentencegame.controller.request.SentenceGameReq;
import com.ssafy.dannae.domain.game.sentencegame.controller.response.SentenceGameRes;
import com.ssafy.dannae.domain.game.sentencegame.service.SentenceGameCommandService;
import com.ssafy.dannae.domain.game.sentencegame.service.SentenceGameQueryService;
import com.ssafy.dannae.domain.game.sentencegame.service.dto.SentenceGameDto;
import com.ssafy.dannae.global.template.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/sentencegames")
@RestController
public class SentenceGameController {

	private final SentenceGameCommandService sentenceGameCommandService;
    private final SentenceGameQueryService sentenceGameQueryService;
	/**
	 * 단어셋을 제시해주는 method
	 * @return active단어셋, inactive단어셋
	 */
	@PostMapping("/{room-id}")
	public ResponseEntity<BaseResponse<?>> createInitial(@PathVariable("room-id") Long roomId) {

        SentenceGameDto sentenceGameDto = SentenceGameDto.builder().roomId(roomId).build();
        SentenceGameDto res = sentenceGameCommandService.createInitial(sentenceGameDto);
		return ResponseEntity.ok(BaseResponse.ofSuccess(res));
	}

	/**
	 * 문장이 제시된 단어를 사용했는지 확인하는 method
	 * @param req : 단어, 문장, 방 번호
	 * @return 사용된 단어, 점수
	 */
	@PatchMapping("/{room-id}")
	public ResponseEntity<BaseResponse<?>> updateWord(@PathVariable("room-id") Long roomId, @RequestBody SentenceGameReq req){
		SentenceGameRes res = sentenceGameCommandService.playGame(req);
		return ResponseEntity.ok(BaseResponse.ofSuccess(res));
	}
}
