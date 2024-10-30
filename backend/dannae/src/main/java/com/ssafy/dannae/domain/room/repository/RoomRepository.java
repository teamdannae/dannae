package com.ssafy.dannae.domain.room.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ssafy.dannae.domain.room.entity.Room;

public interface RoomRepository extends JpaRepository<Room, Long> {

}
