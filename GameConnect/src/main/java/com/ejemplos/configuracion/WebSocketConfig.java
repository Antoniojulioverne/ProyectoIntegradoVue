package com.ejemplos.configuracion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Autowired
    private JwtHandshakeInterceptor jwtHandshakeInterceptor;
    
    @Autowired
    private WebSocketAuthChannelInterceptor webSocketAuthChannelInterceptor;
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        System.out.println("🔧 Configuring Message Broker...");
        
        // Habilitar broker simple para topics y queues
        config.enableSimpleBroker("/topic", "/queue");
        
        // Prefijo para destinos de aplicación
        config.setApplicationDestinationPrefixes("/app");
        
        // Prefijo para destinos de usuario específico
        config.setUserDestinationPrefix("/user");
        
        System.out.println("✅ Message Broker configured");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        System.out.println("🔧 Registering WebSocket endpoints...");
        
        // Endpoint WebSocket nativo (sin SockJS)
        registry.addEndpoint("/ws")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOriginPatterns("*"); // Para desarrollo - en producción usar origins específicos
                
        System.out.println("✅ WebSocket endpoint registered at /ws");
    }
    
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        System.out.println("🔧 Configuring client inbound channel with auth interceptor...");
        
        // Agregar interceptor de autenticación para mensajes entrantes
        registration.interceptors(webSocketAuthChannelInterceptor);
        
        System.out.println("✅ Client inbound channel configured");
    }
}