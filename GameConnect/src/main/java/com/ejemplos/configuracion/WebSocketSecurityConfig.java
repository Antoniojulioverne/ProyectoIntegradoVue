package com.ejemplos.configuracion;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;

@Configuration
public class WebSocketSecurityConfig extends AbstractSecurityWebSocketMessageBrokerConfigurer {

    @Override
    protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
        messages
            // Allow connection without authentication initially
            .nullDestMatcher().permitAll()
            // App destinations require authentication
            .simpDestMatchers("/app/**").authenticated()  
            // Topic subscriptions require authentication
            .simpDestMatchers("/topic/**").authenticated()
            .simpDestMatchers("/queue/**").authenticated()
            .simpDestMatchers("/user/**").authenticated()
            // Allow everything else for now (you can restrict this later)
            .anyMessage().permitAll();
    }

    @Override
    protected boolean sameOriginDisabled() {
        return true; // Allow cross-origin for development
    }
}