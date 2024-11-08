//package com.ssafy.dannae.domain.game.sentencegame.controller;
//
//import com.ssafy.dannae.domain.game.sentencegame.controller.request.InfiniteGameReq;
//import com.ssafy.dannae.domain.game.sentencegame.controller.response.InfiniteGameCreateRes;
//import com.ssafy.dannae.domain.game.sentencegame.controller.response.SentenceGameRes;
//import com.ssafy.dannae.domain.game.sentencegame.service.InfiniteGameCommandService;
//import com.ssafy.dannae.domain.game.sentencegame.service.dto.InfiniteGameDto;
//import com.ssafy.dannae.global.template.response.BaseResponse;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//@Slf4j
//@RequiredArgsConstructor
//@RequestMapping("/api/v1/sentencegames")
//@RestController
//public class SentenceGameController {
//
//	private final InfiniteGameCommandService infiniteGameCommandService;
//	/**
//	 * 단어셋을 제시해주는 method
//	 * @return active단어셋, inactive단어셋
//	 */
//	@PostMapping("/{room-id}")
//	public ResponseEntity<BaseResponse<?>> createInitial(@PathVariable("room-id") Long roomId) {
//
//		InfiniteGameDto infinitegameDto = infiniteGameCommandService.createInitial(InfiniteGameDto.builder()
//			.roomId(roomId)
//			.build());
//
//		InfiniteGameCreateRes infinitegameCreateRes = InfiniteGameCreateRes.builder()
//			.id(infinitegameDto.gameId())
//			.initial(infinitegameDto.initial())
//			.build();
//
//		return ResponseEntity.ok(BaseResponse.ofSuccess(infinitegameCreateRes));
//	}
//
//	/**
//	 * 단어가 제시된 초성에 맞는 사전에 존재하는 단어인지 확인하는 method
//	 * @param req : 단어
//	 * @return 단어, 단어 뜻, 난이도
//	 */
//	@PostMapping("/{room-id}/check")
//	public ResponseEntity<BaseResponse<?>> updateWord(@PathVariable("room-id") Long roomId, @RequestBody InfiniteGameReq req){
//		InfiniteGameDto dto = infiniteGameCommandService.updateWord(InfiniteGameDto.builder()
//			.word(req.word())
//			.gameId(req.gameId())
//			.roomId(roomId)
//			.initial(req.initial())
//			.playerId(req.playerId())
//			.build());
//		SentenceGameRes res = SentenceGameRes.builder()
//			.correct(dto.correct())
//			.word(dto.word())
//			.meaning(dto.meaning())
//			.difficulty(dto.difficulty())
//			.build();
//		return ResponseEntity.ok(BaseResponse.ofSuccess(res));
//	}
//}
