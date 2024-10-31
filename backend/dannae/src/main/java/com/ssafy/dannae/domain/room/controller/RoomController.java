package com.ssafy.dannae.domain.room.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ssafy.dannae.global.util.JwtTokenProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ssafy.dannae.domain.room.controller.request.RoomReq;
import com.ssafy.dannae.domain.room.service.RoomCommandService;
import com.ssafy.dannae.domain.room.service.RoomQueryService;
import com.ssafy.dannae.domain.room.service.dto.RoomDto;
import com.ssafy.dannae.global.template.response.BaseResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/room")
@RestController
public class RoomController {

	private final JwtTokenProvider jwtTokenProvider;
	private final RoomCommandService roomCommandService;
	private final RoomQueryService roomQueryService;

	@PostMapping("")
	public ResponseEntity<BaseResponse<Map<String, Object>>> createRoom(@RequestBody RoomReq req, @RequestParam String playerId){
		RoomDto res = roomQueryService.createRoom(RoomDto.builder()
				.title(req.title())
				.mode(req.mode())
				.release(req.release())
				.build());

		String token = jwtTokenProvider.createToken(res.roomId().toString(), playerId);

		Map<String, Object> response = new HashMap<>();
		response.put("room", res);
		response.put("token", token);

		return ResponseEntity.ok(BaseResponse.ofSuccess(response));
	}

	@PatchMapping("/{room-id}")
	public ResponseEntity<BaseResponse<?>> updateRoom(
		@PathVariable("room-id") Long roomId,
		@RequestBody RoomReq req){
		roomQueryService.updateRoom(roomId, RoomDto.builder()
				.title(req.title())
				.mode(req.mode())
				.release(req.release())
				.build());
		return ResponseEntity.ok(BaseResponse.ofSuccess());
	}

	@GetMapping("/list")
	public ResponseEntity<BaseResponse<List<RoomDto>>> getReleasedRooms(){
		List<RoomDto> res = roomCommandService.getReleasedRooms();
		return ResponseEntity.ok(BaseResponse.ofSuccess(res));
	}

}
