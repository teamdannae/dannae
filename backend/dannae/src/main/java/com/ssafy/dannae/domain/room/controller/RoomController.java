package com.ssafy.dannae.domain.room.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ssafy.dannae.domain.room.controller.request.RoomCreateReq;
import com.ssafy.dannae.domain.room.controller.request.RoomReq;
import com.ssafy.dannae.domain.room.controller.response.RoomCreateRes;
import com.ssafy.dannae.domain.room.service.RoomCommandService;
import com.ssafy.dannae.domain.room.service.RoomQueryService;
import com.ssafy.dannae.domain.room.service.dto.RoomDetailDto;
import com.ssafy.dannae.domain.room.service.dto.RoomDto;
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
		jwtTokenDecoder.getPlayerId(token);
		return ResponseEntity.ok(BaseResponse.ofSuccess(roomQueryService.readUnreleasedRoom(code)));

	}

	@GetMapping("/list")
	public ResponseEntity<BaseResponse<List<?>>> readReleasedRooms(){

		List<RoomDto> res = roomQueryService.readReleasedRooms();
		return ResponseEntity.ok(BaseResponse.ofSuccess(res));

	}

	@GetMapping("/{room-id}")
	public ResponseEntity<BaseResponse<?>> readRoom(@PathVariable("room-id") Long roomId){

		RoomDetailDto res = roomQueryService.readDetail(roomId);
		return ResponseEntity.ok(BaseResponse.ofSuccess(res));

	}

}
