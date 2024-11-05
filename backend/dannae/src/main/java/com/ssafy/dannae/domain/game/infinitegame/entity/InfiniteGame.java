package com.ssafy.dannae.domain.game.infinitegame.entity;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "initial_consonant")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InfiniteGame {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "initial_consonant_id")
	private Long id;

	@Column(name = "room_id", nullable = false)
	private Long roomId;

	@Column(name = "initial", nullable = false)
	private String initial;

	@JdbcTypeCode(SqlTypes.ARRAY)
	@Column(name = "list", columnDefinition = "text[]")
	private List<String> list;

	/**
	 * 방 아이디와 초성값을 받아서 객체를 생성
	 */
	@Builder
	public InfiniteGame(Long roomId, String initial) {
		this(roomId, initial, new ArrayList<>());
	}


	public InfiniteGame(Long roomId, String initial, List<String> list) {
		this.roomId = roomId;
		this.initial = initial;
		this.list = list;
	}

	/**
	 * 방에서 나온 단어 리스트를 업데이트 해준다.
	 * @param content
	 */
	public void updateList(String content){
		this.list.add(content);
	}

}
