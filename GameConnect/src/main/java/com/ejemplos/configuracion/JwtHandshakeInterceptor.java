package com.ejemplos.configuracion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request, 
            ServerHttpResponse response,
            WebSocketHandler wsHandler, 
            Map<String, Object> attributes) throws Exception {
        
        System.out.println("=== WebSocket Handshake Started ===");
        System.out.println("Request URI: " + request.getURI());
        
        try {
            // Extraer token de la query string
            URI uri = request.getURI();
            String query = uri.getQuery();
            String token = null;
            
            if (query != null) {
                // Buscar el parámetro token en la query string
                String[] params = query.split("&");
                for (String param : params) {
                    if (param.startsWith("token=")) {
                        token = param.substring(6); // "token=".length() = 6
                        break;
                    }
                }
            }
            
            // También intentar extraer del header Authorization
            if (token == null) {
                String authHeader = request.getHeaders().getFirst("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                }
            }
            
            System.out.println("Token extracted: " + (token != null ? "Present" : "Missing"));
            
            if (token == null) {
                System.err.println("❌ No JWT token provided");
                return false;
            }
            
            // Validar token
            if (!jwtUtil.validateToken(token)) {
                System.err.println("❌ Invalid JWT token");
                return false;
            }
            
            String username = jwtUtil.extractUsername(token);
            System.out.println("✅ Token valid for user: " + username);
            
            // Guardar token y usuario en los atributos de la sesión WebSocket
            attributes.put("token", token);
            attributes.put("username", username);
            
            System.out.println("✅ Handshake successful for user: " + username);
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ Error during WebSocket handshake: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request, 
            ServerHttpResponse response,
            WebSocketHandler wsHandler, 
            Exception exception) {
        
        if (exception != null) {
            System.err.println("❌ WebSocket handshake failed: " + exception.getMessage());
        } else {
            System.out.println("✅ WebSocket handshake completed successfully");
        }
    }
}