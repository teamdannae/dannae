package com.ssafy.dannae.domain.room.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

	private final RoomCommandService roomCommandService;
	private final RoomQueryService roomQueryService;

	@PostMapping("")
	public ResponseEntity<BaseResponse<RoomDto>> createRoom(@RequestBody RoomReq req){
		RoomDto res = roomQueryService.createRoom(RoomDto.builder()
			.title(req.title())
			.mode(req.mode())
			.release(req.release())
			.build());
		return ResponseEntity.ok(BaseResponse.ofSuccess(res));
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
