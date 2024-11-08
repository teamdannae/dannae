package com.ssafy.dannae.domain.room.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ssafy.dannae.domain.room.entity.Room;
import com.ssafy.dannae.domain.room.entity.RoomStatus;

public interface RoomRepository extends JpaRepository<Room, Long> {


	List<Room> findByReleaseAndStatusOrderByIdDesc(Boolean release, RoomStatus status);

	boolean existsById(Long id);

}
