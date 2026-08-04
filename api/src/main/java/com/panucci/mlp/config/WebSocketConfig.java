package com.panucci.mlp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer{
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Destinos usados para o servidor publicar mensagens.
        registry.enableSimpleBroker("/topic");

        // Prefixo das mensagens enviadas pelo cliente aos controllers.
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint usado apenas para estabelecer a conexão WebSocket.
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }
}
