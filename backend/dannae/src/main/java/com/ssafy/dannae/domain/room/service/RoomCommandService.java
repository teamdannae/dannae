package com.ssafy.dannae.domain.room.service;

import com.ssafy.dannae.domain.room.service.dto.RoomDto;

public interface RoomCommandService {

	RoomDto createRoom(RoomDto roomDto);

	void updateRoom(Long roomId, RoomDto roomDto);

}
