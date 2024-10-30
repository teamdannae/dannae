package com.ssafy.dannae.domain.room.service;

import java.util.List;

import com.ssafy.dannae.domain.room.service.dto.RoomDto;

public interface RoomCommandService {

	List<RoomDto> getReleasedRooms();

}
