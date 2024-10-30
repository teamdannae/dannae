package com.ssafy.dannae.domain.room.service.Impl;

import java.util.Random;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.dannae.domain.room.entity.Room;
import com.ssafy.dannae.domain.room.exception.NoRoomException;
import com.ssafy.dannae.domain.room.repository.RoomRepository;
import com.ssafy.dannae.domain.room.service.RoomQueryService;
import com.ssafy.dannae.domain.room.service.dto.RoomDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
class RoomQueryServiceImpl implements RoomQueryService {

	private final RoomRepository roomRepository;

	@Override
	public RoomDto createRoom(RoomDto dto) {
		String randomCode = String.valueOf(100000 + new Random().nextInt(900000));
		Room room = roomRepository.save(Room.builder()
			.title(dto.title())
			.mode(dto.mode())
			.code(randomCode)
			.release(dto.release())
			.build());
		return RoomDto.builder()
			.roomId(room.getId())
			.build();
	}

	@Override
	public void updateRoom(Long roomId, RoomDto dto) {
		Room room = roomRepository.findById(roomId)
			.orElseThrow(() -> new 	NoRoomException("Room not found"));
		room.update(dto.title(), dto.mode(), dto.release());
	}

}
