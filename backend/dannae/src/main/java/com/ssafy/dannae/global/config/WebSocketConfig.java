package com.ssafy.dannae.global.config;

import com.ssafy.dannae.global.exception.handler.SentenceGameWebSocketHandler;
import com.ssafy.dannae.global.exception.handler.GameWebSocketHandler;
import com.ssafy.dannae.global.exception.handler.WaitingRoomWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final GameWebSocketHandler gameWebSocketHandler;
    private final WaitingRoomWebSocketHandler waitingRoomWebSocketHandler;
    private final SentenceGameWebSocketHandler sentenceGameWebSocketHandler;

    public WebSocketConfig(GameWebSocketHandler gameWebSocketHandler, WaitingRoomWebSocketHandler waitingRoomWebSocketHandler,  SentenceGameWebSocketHandler sentenceGameWebSocketHandler) {
        this.gameWebSocketHandler = gameWebSocketHandler;
        this.waitingRoomWebSocketHandler = waitingRoomWebSocketHandler;
        this.sentenceGameWebSocketHandler = sentenceGameWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(gameWebSocketHandler, "/ws/infinitegame")
                .addInterceptors(new HttpSessionHandshakeInterceptor())
                .setAllowedOriginPatterns("*");

        registry.addHandler(waitingRoomWebSocketHandler, "/ws/waitingroom")
                .addInterceptors(new HttpSessionHandshakeInterceptor())
                .setAllowedOriginPatterns("*");

        registry.addHandler(sentenceGameWebSocketHandler, "/ws/sentencegame")
                .addInterceptors(new HttpSessionHandshakeInterceptor())
                .setAllowedOriginPatterns("*");
    }
}