package com.ssafy.dannae.domain.game.sentencegame.entity;

import com.ssafy.dannae.domain.game.entity.Word;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.redis.core.RedisHash;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@RedisHash("SentenceGame")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SentenceGame {

	@Id
	private Long roomId;

	private Set<String> activeWords = new HashSet<>();

	private Set<String> inactiveWords = new HashSet<>();

	/**
	 * 방 아이디를 받아서 객체를 생성
	 */
	@Builder
	public SentenceGame(Long roomId, Set<String> activeWords, Set<String> inactiveWords) {
		this.roomId = roomId;
		this.activeWords = activeWords;
		this.inactiveWords = inactiveWords;
	}


	/**
	 * 방에서 나온 단어 리스트를 업데이트 해준다.
	 * @param content
	 */
//	public void updateList(String content){
//		this.list.add(content);
//	}

}
