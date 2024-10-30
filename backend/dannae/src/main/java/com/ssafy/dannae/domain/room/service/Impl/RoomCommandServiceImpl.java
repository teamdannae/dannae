package com.ssafy.dannae.domain.room.service.Impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.dannae.domain.room.entity.Room;
import com.ssafy.dannae.domain.room.repository.RoomRepository;
import com.ssafy.dannae.domain.room.service.RoomCommandService;
import com.ssafy.dannae.domain.room.service.dto.RoomDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
class RoomCommandServiceImpl implements RoomCommandService {

	private final RoomRepository roomRepository;

	@Override
	public List<RoomDto> getReleasedRooms() {
		List<Room> roomList = roomRepository.findByRelease("true"); // release가 "true"인 방만 조회
		return roomList.stream()
			.map(room -> RoomDto.builder()
				.roomId(room.getId())
				.title(room.getTitle())
				.mode(room.getMode())
				.release(room.getRelease())
				.build())
			.collect(Collectors.toList());
	}

}
