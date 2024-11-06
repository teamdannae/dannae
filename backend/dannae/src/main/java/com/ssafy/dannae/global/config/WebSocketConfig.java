package com.ssafy.dannae.global.config;

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

    public WebSocketConfig(GameWebSocketHandler gameWebSocketHandler, WaitingRoomWebSocketHandler waitingRoomWebSocketHandler) {
        this.gameWebSocketHandler = gameWebSocketHandler;
        this.waitingRoomWebSocketHandler = waitingRoomWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(gameWebSocketHandler, "/ws/game")
                .addInterceptors(new HttpSessionHandshakeInterceptor())
                .setAllowedOriginPatterns("*");

        registry.addHandler(waitingRoomWebSocketHandler, "/ws/waitingroom")
                .addInterceptors(new HttpSessionHandshakeInterceptor())
                .setAllowedOriginPatterns("*");
    }
}