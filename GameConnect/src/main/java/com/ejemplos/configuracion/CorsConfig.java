package com.ejemplos.configuracion;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
public class CorsConfig implements WebMvcConfigurer {

	 @Override
	    public void addCorsMappings(CorsRegistry registry) {
	        registry.addMapping("/**")
	                // Orígenes específicos para Capacitor
	                .allowedOriginPatterns(
	                    "*",
	                    "https://localhost",
	                    "https://localhost:*",
	                    "http://localhost",
	                    "http://localhost:*",
	                    "capacitor://localhost",
	                    "ionic://localhost",
	                    "http://192.168.1.*:*",
	                    "https://192.168.1.*:*"
	                )
	                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH")
	                .allowedHeaders(
	                    "*",
	                    "Origin",
	                    "X-Requested-With",
	                    "Content-Type",
	                    "Accept",
	                    "Authorization",
	                    "Access-Control-Allow-Origin",
	                    "Access-Control-Allow-Headers",
	                    "Access-Control-Allow-Methods"
	                )
	                .exposedHeaders(
	                    "Access-Control-Allow-Origin",
	                    "Access-Control-Allow-Credentials",
	                    "Authorization"
	                )
	                .allowCredentials(true)
	                .maxAge(3600);
	        
	        // Logging para debug
	        System.out.println("🌐 CORS Configuration loaded:");
	        System.out.println("   - Allowed Origins: *, https://localhost, capacitor://localhost");
	        System.out.println("   - Allowed Methods: GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH");
	        System.out.println("   - Allow Credentials: true");
	    }
}