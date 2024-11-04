package com.ssafy.dannae.domain.room.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ssafy.dannae.domain.player.entity.PlayerAuthorization;
import com.ssafy.dannae.domain.player.entity.PlayerStatus;
import com.ssafy.dannae.domain.player.service.PlayerQueryService;
import com.ssafy.dannae.domain.player.service.dto.PlayerDto;
import com.ssafy.dannae.domain.room.controller.request.RoomCreaterReq;
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
	private final PlayerQueryService playerQueryService;

	@PostMapping("")
	public ResponseEntity<BaseResponse<Map<String, Object>>> createRoom(@RequestBody RoomCreaterReq req){
		RoomDto roomDto= roomQueryService.createRoom(RoomDto.builder()
				.title(req.title())
				.mode(req.mode())
				.release(req.release())
				.build());

		Long roomId = roomDto.roomId();

		PlayerDto playerDto = playerQueryService.createPlayer(PlayerDto.builder()
				.roomId(roomId)
				.score(0L)
				.status(String.valueOf(PlayerStatus.nonready))
				.authorization(String.valueOf(PlayerAuthorization.creator))
				.nickname(req.nickname())
				.image(req.image())
				.build());

		String token = jwtTokenProvider.createToken( roomId.toString(),playerDto.playerId().toString());

		Map<String, Object> response = new HashMap<>();
		response.put("roomId", roomDto.roomId());
		response.put("playerId", playerDto.playerId());
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
