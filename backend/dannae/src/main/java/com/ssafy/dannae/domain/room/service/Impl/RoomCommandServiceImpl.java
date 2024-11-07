package com.ssafy.dannae.domain.room.service.Impl;

import java.util.Random;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.dannae.domain.room.entity.Room;
import com.ssafy.dannae.domain.room.exception.NoRoomException;
import com.ssafy.dannae.domain.room.repository.RoomRepository;
import com.ssafy.dannae.domain.room.service.RoomCommandService;
import com.ssafy.dannae.domain.room.service.dto.RoomDetailDto;
import com.ssafy.dannae.domain.room.service.dto.RoomDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
class RoomCommandServiceImpl implements RoomCommandService {

	private final RoomRepository roomRepository;

	@Override
	public RoomDto createRoom(RoomDetailDto dto) {

		String randomCode = String.valueOf(100000 + new Random().nextInt(900000));

		Room room = roomRepository.save(Room.builder()
			.title(dto.title())
			.mode(dto.mode())
			.code(randomCode)
			.release(dto.isPublic())
			.creator(dto.creator())
			.build());

		return RoomDto.builder()
			.roomId(room.getId())
			.build();

	}

	@Override
	public void updateRoom(Long roomId, RoomDetailDto dto) {

		Room room = verifiedById(roomId);
		room.update(dto.title(), dto.mode(), dto.isPublic());
		roomRepository.save(room);
	}

	@Override
	public void updateRoomCreator(Long roomId, Long roomCreatorId) {

		Room room = verifiedById(roomId);
		room.updateCreator(roomCreatorId);
		roomRepository.save(room);

	}

	@Override
	public void updatePlayerCount(Long roomId, Long roomPlayerCount) {

		Room room = verifiedById(roomId);
		room.updatePlayerCount(roomPlayerCount);
		roomRepository.save(room);

	}

	private Room verifiedById(Long roomId) {

		Room room = roomRepository.findById(roomId)
			.orElseThrow(() -> new NoRoomException("Room not found"));

		return room;
	}

}
