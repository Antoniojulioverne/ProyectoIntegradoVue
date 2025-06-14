package com.ejemplos.configuracion;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {
    
    // Generar una clave segura para HS256
    private final SecretKey SECRET_KEY = Keys.hmacShaKeyFor("mi-clave-super-secreta-que-debe-tener-al-menos-32-caracteres".getBytes());
    
    public String generateToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis()))
               // .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24)) // 1 dia
                .signWith(SECRET_KEY)
                .compact();
    }
    
    public String extractUsername(String token) {
        return Jwts.parser()
                .verifyWith(SECRET_KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(SECRET_KEY)
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}