package com.ssafy.dannae.domain.room.service;

import com.ssafy.dannae.domain.room.service.dto.RoomDetailDto;
import com.ssafy.dannae.domain.room.service.dto.RoomDto;

public interface RoomCommandService {

	RoomDto createRoom(RoomDetailDto roomDetailDto);

	void updateRoom(Long roomId, RoomDetailDto roomDetailDto);

	void updatePlayerCount(Long roomId, Long playerCount);

	void updateRoomCreator(Long roomId, Long roomCreatorId);

}
