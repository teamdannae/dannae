package com.ssafy.dannae.domain.room.service.Impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.dannae.domain.player.exception.NoPlayerException;
import com.ssafy.dannae.domain.player.repository.PlayerRepository;
import com.ssafy.dannae.domain.room.entity.Room;
import com.ssafy.dannae.domain.room.entity.RoomStatus;
import com.ssafy.dannae.domain.room.exception.NoRoomException;
import com.ssafy.dannae.domain.room.repository.RoomRepository;
import com.ssafy.dannae.domain.room.service.RoomQueryService;
import com.ssafy.dannae.domain.room.service.dto.RoomDetailDto;
import com.ssafy.dannae.domain.room.service.dto.RoomDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
class RoomQueryServiceImpl implements RoomQueryService {

	private final RoomRepository roomRepository;
	private final PlayerRepository playerRepository;

	@Override
	public List<RoomDto> readReleasedRooms() {

		List<Room> roomList = roomRepository.findByReleaseAndStatusOrderByIdDesc(true, RoomStatus.READY);

		return roomList.stream()
			.map(room -> RoomDto.builder()
				.roomId(room.getId())
				.title(room.getTitle())
				.mode(room.getMode())
				.playerCount(room.getPlayerCount())
				.creator(room.getCreator())
				.creatorNickname(
					playerRepository.findById(room.getCreator())
						.orElseThrow(() -> new NoPlayerException("없는 사용자입니다."))
						.getNickname())
				.build())
			.collect(Collectors.toList());
	}

	@Override
	public RoomDetailDto readDetail(Long roomId) {

		Room room = findById(roomId).orElseThrow(() -> new NoRoomException("Room not found"));

		RoomDetailDto dto = RoomDetailDto.builder()
			.roomId(room.getId())
			.title(room.getTitle())
			.mode(room.getMode())
			.isPublic(room.getRelease())
			.code(room.getCode())
			.playerCount(room.getPlayerCount())
			.creator(room.getCreator())
			.build();

		return dto;
	}

	@Override
	public Optional<Room> findById(Long roomId) {
		return roomRepository.findById(roomId);
	}

	@Override
	public boolean existsById(Long roomId) {
		return roomRepository.existsById(roomId);
	}

}
