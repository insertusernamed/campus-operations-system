package org.campusscheduler.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time solver progress updates.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {
		// Enable a simple in-memory message broker for /topic destinations
		config.enableSimpleBroker("/topic");
		// Prefix for messages from clients to server
		config.setApplicationDestinationPrefixes("/app");
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		// WebSocket endpoint - clients connect here
		// Origins aligned with CorsConfig.java
		registry.addEndpoint("/ws")
				.setAllowedOrigins(
						"http://localhost:5173",
						"http://localhost:3000",
						"http://127.0.0.1:5173")
				.withSockJS(); // Fallback for browsers without WebSocket
	}
}
