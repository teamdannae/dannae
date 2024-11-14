package com.ssafy.dannae.domain.room.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.dannae.domain.player.service.PlayerCommandService;
import com.ssafy.dannae.domain.player.service.PlayerQueryService;
import com.ssafy.dannae.domain.room.controller.request.RoomCreateReq;
import com.ssafy.dannae.domain.room.controller.request.RoomReq;
import com.ssafy.dannae.domain.room.controller.response.RoomCreateRes;
import com.ssafy.dannae.domain.room.service.RoomCommandService;
import com.ssafy.dannae.domain.room.service.RoomQueryService;
import com.ssafy.dannae.domain.room.service.dto.RoomDetailDto;
import com.ssafy.dannae.domain.room.service.dto.RoomDto;
import com.ssafy.dannae.global.exception.ResponseCode;
import com.ssafy.dannae.global.template.response.BaseResponse;
import com.ssafy.dannae.global.util.JwtTokenDecoder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/rooms")
@RestController
public class RoomController {

	private final RoomCommandService roomCommandService;
	private final RoomQueryService roomQueryService;
	private final PlayerQueryService playerQueryService;
	private final PlayerCommandService playerCommandService;
	private final JwtTokenDecoder jwtTokenDecoder;

	@PostMapping("")
	public ResponseEntity<BaseResponse<?>> createRoom(@RequestHeader(HttpHeaders.AUTHORIZATION) String token, @RequestBody RoomCreateReq req){

		Long creator = jwtTokenDecoder.getPlayerId(token);

		RoomDto roomDto = roomCommandService.createRoom(RoomDetailDto.builder()
				.title(req.title())
				.mode(req.mode())
				.isPublic(req.isPublic())
				.creator(creator)
				.build());

		RoomCreateRes roomCreateRes = RoomCreateRes.builder()
			.roomId(roomDto.roomId())
			.build();

		return ResponseEntity.ok(BaseResponse.ofSuccess(roomCreateRes));

	}

	@PatchMapping("/{room-id}")
	public ResponseEntity<BaseResponse<?>> updateRoom(@PathVariable("room-id") Long roomId,	@RequestBody RoomReq req){

		roomCommandService.updateRoom(roomId, RoomDetailDto.builder()
				.title(req.title())
				.mode(req.mode())
				.isPublic(req.isPublic())
				.build());

		return ResponseEntity.ok(BaseResponse.ofSuccess());

	}

	@GetMapping("/check-code")
	public ResponseEntity<BaseResponse<Long>> checkRoomCode(@RequestParam() String code,
															@RequestHeader(HttpHeaders.AUTHORIZATION) String token){
		jwtTokenDecoder.validateToken(token);
		return ResponseEntity.ok(BaseResponse.ofSuccess(roomQueryService.readUnreleasedRoom(code)));

	}

	@GetMapping("/list")
	public ResponseEntity<BaseResponse<List<?>>> readReleasedRooms(@RequestHeader(HttpHeaders.AUTHORIZATION) String token){
		jwtTokenDecoder.validateToken(token);
		List<RoomDto> res = roomQueryService.readReleasedRooms();
		return ResponseEntity.ok(BaseResponse.ofSuccess(res));

	}

	@GetMapping("/{room-id}")
	public ResponseEntity<BaseResponse<?>> readRoom(@PathVariable("room-id") Long roomId,
													@RequestHeader(HttpHeaders.AUTHORIZATION) String token){
		long playerId = jwtTokenDecoder.getPlayerId(token);
		if(roomQueryService.isPlayingRoom(roomId)){
			RoomDetailDto res = roomQueryService.readDetail(roomId);
			playerCommandService.resetScore(playerId);
			return ResponseEntity.ok(BaseResponse.ofSuccess(res));
		}
		if(playerQueryService.canEnterRoom(playerId)) {
			RoomDetailDto res = roomQueryService.readDetail(roomId);
			return ResponseEntity.ok(BaseResponse.ofSuccess(res));
		}

		return ResponseEntity
				.status(HttpStatus.BAD_REQUEST)
				.body(BaseResponse.ofFail(ResponseCode.ALREADY_IN_ROOM_EXCEPTION));

	}

}
