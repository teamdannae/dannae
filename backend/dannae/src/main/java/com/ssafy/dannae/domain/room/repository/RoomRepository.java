package com.ssafy.dannae.domain.room.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ssafy.dannae.domain.room.entity.Room;

public interface RoomRepository extends JpaRepository<Room, Long> {

	List<Room> findByRelease(Boolean release);

	boolean existsById(Long id);

	Optional<Room> findById(Long id);
}
