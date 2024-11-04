package com.ssafy.dannae.domain.room.service;

import com.ssafy.dannae.domain.room.service.dto.RoomDto;

public interface RoomQueryService {

	RoomDto createRoom(RoomDto roomDto);

	void updateRoom(Long roomId, RoomDto roomDto);

	boolean existsById(Long roomId);
}
