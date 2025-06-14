package com.ejemplos.configuracion;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.ejemplos.modelo.UsuarioRepositorio;

import java.io.IOException;
import java.util.ArrayList;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        System.out.println("=== DEBUGGING JWT FILTER ===");
        System.out.println("URL: " + request.getRequestURI());
        
        try {
            String authHeader = request.getHeader("Authorization");
            System.out.println("Authorization header: " + authHeader);

            String token = null;

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
                System.out.println("Token extraído del header Authorization: " + token);
            } else {
                // Si no hay header Authorization, revisamos el query param 'token'
                token = request.getParameter("token");
                if (token != null) {
                    System.out.println("Token extraído del query param: " + token);
                } else {
                    System.out.println("❌ No hay Authorization header ni token en query param");
                }
            }

            if (token != null && !token.trim().isEmpty()) {
                try {
                    String email = jwtUtil.extractUsername(token);
                    System.out.println("Email extraído: " + email);
                    
                    boolean isValid = jwtUtil.validateToken(token);
                    System.out.println("Token válido: " + isValid);

                    if (email != null && isValid && SecurityContextHolder.getContext().getAuthentication() == null) {
                        UsernamePasswordAuthenticationToken authToken = 
                            new UsernamePasswordAuthenticationToken(email, null, new ArrayList<>());
                        
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        request.setAttribute("userEmail", email);
                        System.out.println("✅ Usuario autenticado en Spring Security: " + email);
                    } else {
                        System.out.println("❌ Token inválido, email null, o ya autenticado");
                    }
                } catch (Exception e) {
                    System.err.println("❌ Error procesando JWT: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println("❌ Token vacío");
            }

        } catch (Exception e) {
            System.err.println("❌ Error general en JwtRequestFilter: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("=== FIN DEBUG JWT FILTER ===");
        filterChain.doFilter(request, response);
    }


    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return path.startsWith("/auth/") ||
               path.equals("/") ||
               path.startsWith("/public/") ||
               path.startsWith("/error");
    }
}