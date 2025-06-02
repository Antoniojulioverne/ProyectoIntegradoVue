package com.ejemplos.configuracion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor == null) {
            return message;
        }

        System.out.println("=== WebSocket Interceptor Debug ===");
        System.out.println("Command: " + accessor.getCommand());
        System.out.println("Destination: " + accessor.getDestination());
        
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            System.out.println("Processing CONNECT command");
            
            // Get token from session attributes (set by handshake interceptor)
            String token = (String) accessor.getSessionAttributes().get("token");
            System.out.println("Token from session: " + (token != null ? "Present" : "Missing"));
            
            if (token == null) {
                System.err.println("❌ No JWT token provided in handshake");
                throw new SecurityException("Token JWT no proporcionado en handshake");
            }

            try {
                if (jwtUtil.validateToken(token)) {
                    String username = jwtUtil.extractUsername(token);
                    System.out.println("✅ Token valid for user: " + username);
                    
                    Authentication auth = new UsernamePasswordAuthenticationToken(
                        username, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
                    
                    accessor.setUser(auth);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    System.out.println("✅ User authenticated in WebSocket: " + username);
                } else {
                    System.err.println("❌ Invalid JWT token");
                    throw new SecurityException("Token JWT inválido");
                }
            } catch (Exception e) {
                System.err.println("❌ Error in WebSocket authentication: " + e.getMessage());
                throw new SecurityException("Error en autenticación WebSocket: " + e.getMessage());
            }
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand()) || 
                   StompCommand.SEND.equals(accessor.getCommand())) {
            
            // For other commands, ensure user is still authenticated
            Authentication auth = (Authentication) accessor.getUser();
            if (auth == null) {
                System.err.println("❌ No authentication found for command: " + accessor.getCommand());
                throw new SecurityException("Usuario no autenticado para WebSocket");
            }
            
            // Set security context for this thread
            SecurityContextHolder.getContext().setAuthentication(auth);
            System.out.println("✅ Authentication set for command: " + accessor.getCommand());
        }
        
        System.out.println("=== End WebSocket Interceptor Debug ===");
        return message;
    }
}