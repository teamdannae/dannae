package com.ssafy.dannae.domain.game.infinitegame.service.dto;

import org.springframework.web.socket.WebSocketSession;

public class SubmittedAnswer {
	private final Long playerId;
	private final String answer;
	private final WebSocketSession session;

	public SubmittedAnswer(Long playerId, String answer, WebSocketSession session) {
		this.playerId = playerId;
		this.answer = answer;
		this.session = session;
	}

	public Long getPlayerId() {
		return playerId;
	}

	public String getAnswer() {
		return answer;
	}

	public WebSocketSession getSession() {
		return session;
	}
}
