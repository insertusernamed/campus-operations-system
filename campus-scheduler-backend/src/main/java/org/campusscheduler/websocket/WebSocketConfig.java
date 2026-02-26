package org.campusscheduler.websocket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

/**
 * WebSocket configuration for real-time solver progress updates.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	@Value("${cors.allowed-origins}")
	private List<String> allowedOrigins;

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
		// Origins read from cors.allowed-origins property (aligned with CorsConfig.java)
		registry.addEndpoint("/ws")
				.setAllowedOrigins(allowedOrigins.toArray(String[]::new))
				.withSockJS(); // Fallback for browsers without WebSocket
	}
}
