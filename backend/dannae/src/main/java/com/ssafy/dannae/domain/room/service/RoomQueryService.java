package com.ssafy.dannae.domain.room.service;

import java.util.List;
import java.util.Optional;

import com.ssafy.dannae.domain.room.entity.Room;
import com.ssafy.dannae.domain.room.service.dto.RoomDetailDto;
import com.ssafy.dannae.domain.room.service.dto.RoomDto;

public interface RoomQueryService {

	List<RoomDto> readReleasedRooms();

	boolean existsById(Long roomId);

	RoomDetailDto readDetail(Long roomId);

	Optional<Room> findById(Long roomId);

	Long readUnreleasedRoom(String code);

	boolean isPlayingRoom(Long roomId);
}
