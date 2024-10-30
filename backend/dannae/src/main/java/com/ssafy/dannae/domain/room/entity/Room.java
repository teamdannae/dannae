package com.ssafy.dannae.domain.room.entity;

import com.ssafy.dannae.domain.room.exception.TitleNumberOverflowException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "room")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Room {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "room_id")
	private Long id;


	@Column(name = "title", nullable = false)
	private String title;

	@Column(name = "mode", nullable = false)
	private String mode;

	@Column(name = "release", nullable = false)
	private String release;

	@Column(name = "code", nullable = false)
	private String code;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private RoomStatus status;

	@Builder
	public Room(String title, String mode, String release, String code) {
		this(title, mode, release, code, RoomStatus.READY);
	}

	public Room(String title, String mode, String release, String code, RoomStatus status) {
		checkRoomTitle(title);
		this.title = title;
		this.mode = mode;
		this.release = release;
		this.code = code;
		this.status = status;
	}

	private void checkRoomTitle(String title) {
		if (title.length() > 20) {
			throw new TitleNumberOverflowException("Invalid Title value");
		}
	}

	public void updateStatus() {
		if(checkRoomStatus() == RoomStatus.READY) {
			this.status = RoomStatus.PLAYING;
			return;
		}
		this.status = RoomStatus.READY;
	}

	private RoomStatus checkRoomStatus() {
		return this.status;
	}

	public void update(String title, String mode, String release){
		this.title = title;
		this.mode = mode;
		this.release = release;
	}

}
