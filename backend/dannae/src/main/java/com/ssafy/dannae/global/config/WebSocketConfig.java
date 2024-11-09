package com.ssafy.dannae.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import com.ssafy.dannae.global.exception.handler.InfiniteGameWebSocketHandler;
import com.ssafy.dannae.global.exception.handler.SentenceGameWebSocketHandler;
import com.ssafy.dannae.global.exception.handler.WaitingRoomWebSocketHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final WaitingRoomWebSocketHandler waitingRoomWebSocketHandler;
    private final SentenceGameWebSocketHandler sentenceGameWebSocketHandler;
    private final InfiniteGameWebSocketHandler infiniteGameWebSocketHandler;

    public WebSocketConfig( WaitingRoomWebSocketHandler waitingRoomWebSocketHandler,  SentenceGameWebSocketHandler sentenceGameWebSocketHandler,
		InfiniteGameWebSocketHandler infiniteGameWebSocketHandler) {
        this.waitingRoomWebSocketHandler = waitingRoomWebSocketHandler;
        this.sentenceGameWebSocketHandler = sentenceGameWebSocketHandler;
		this.infiniteGameWebSocketHandler = infiniteGameWebSocketHandler;
	}

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {

        registry.addHandler(waitingRoomWebSocketHandler, "/ws/waitingroom")
                .addInterceptors(new HttpSessionHandshakeInterceptor())
                .setAllowedOriginPatterns("*");

        registry.addHandler(sentenceGameWebSocketHandler, "/ws/sentencegame")
                .addInterceptors(new HttpSessionHandshakeInterceptor())
                .setAllowedOriginPatterns("*");

        registry.addHandler(infiniteGameWebSocketHandler, "/ws/infinitegame")
            .addInterceptors(new HttpSessionHandshakeInterceptor())
            .setAllowedOriginPatterns("*");
    }
}
