package com.ssafy.dannae.domain.room.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ssafy.dannae.domain.room.entity.Room;

public interface RoomRepository extends JpaRepository<Room, Long> {

	List<Room> findByRelease(String release);

	boolean existsById(Long id);
}
