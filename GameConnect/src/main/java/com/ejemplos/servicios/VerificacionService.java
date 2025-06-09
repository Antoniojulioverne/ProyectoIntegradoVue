package com.ejemplos.servicios;

import com.ejemplos.modelo.Usuario;
import com.ejemplos.modelo.UsuarioRepositorio;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@RequiredArgsConstructor
public class VerificacionService {
    
    private final UsuarioRepositorio usuarioRepositorio;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder; // AGREGADO: Para encriptar contraseñas
    
    public String generarCodigoVerificacion() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }
    

    
    public void enviarCodigoVerificacion(Usuario usuario) {
        String codigo = generarCodigoVerificacion();
        usuario.setCodigoVerificacion(codigo);
        usuario.setFechaExpiracionCodigo(LocalDateTime.now().plusHours(24));
        usuarioRepositorio.save(usuario);
        
        emailService.enviarCodigoVerificacion(usuario.getEmail(), codigo);
    }
    
    public boolean verificarCodigo(String codigo) {
        Usuario usuario = usuarioRepositorio.findByCodigoVerificacion(codigo);
        
        if (usuario == null) {
            return false;
        }
        
        // Verificar si el código no ha expirado
        if (LocalDateTime.now().isAfter(usuario.getFechaExpiracionCodigo())) {
            return false;
        }
        
        // ACTUALIZACIÓN: Establecer fecha de creación cuando se verifica
        usuario.setEmailVerificado(true);
        usuario.setFechaCreacion(LocalDateTime.now()); // ¡Nueva línea!
        usuario.setCodigoVerificacion(null);
        usuario.setFechaExpiracionCodigo(null);
        
        usuarioRepositorio.save(usuario);
        
        return true;
    }
    
    public boolean enviarTokenRecuperacion(String email) {
        Usuario usuario = usuarioRepositorio.findByEmail(email);
        
        if (usuario == null || !usuario.isEmailVerificado()) {
            return false;
        }
        
        // Generar token único
        String token = UUID.randomUUID().toString();
        
        // Guardar token directamente en el usuario
        usuario.setTokenRecuperacion(token);
        usuario.setFechaExpiracionToken(LocalDateTime.now().plusHours(1)); // Expira en 1 hora
        usuarioRepositorio.save(usuario);
        
        // IMPORTANTE: El enlace debe apuntar a tu app Ionic
        String enlaceRecuperacion = "http://172.25.9.154:8100/recuperar-contrasena?token=" + token;
        // Para producción: "https://tu-app-ionic.com/reset-password?token=" + token;
        
        // Enviar email con el enlace
        String asunto = "Recuperar contraseña - Game Stats";
        String cuerpoEmail = crearCuerpoEmailRecuperacion(usuario.getUsername(), enlaceRecuperacion);
        
        emailService.enviarEmailRecuperacion(email, asunto, cuerpoEmail);
        
        return true;
    }
    
    public boolean validarTokenRecuperacion(String token) {
        Usuario usuario = usuarioRepositorio.findByTokenRecuperacion(token);
        
        if (usuario == null) {
            return false;
        }
        
        // Verificar si el token ha expirado
        if (usuario.getFechaExpiracionToken() == null || 
            usuario.getFechaExpiracionToken().isBefore(LocalDateTime.now())) {
            // Token expirado, limpiar campos
            usuario.setTokenRecuperacion(null);
            usuario.setFechaExpiracionToken(null);
            usuarioRepositorio.save(usuario);
            return false;
        }
        
        return true;
    }
    
    public boolean restablecerContrasena(String token, String nuevaContrasena) {
        Usuario usuario = usuarioRepositorio.findByTokenRecuperacion(token);
        
        if (usuario == null || usuario.getFechaExpiracionToken() == null ||
            usuario.getFechaExpiracionToken().isBefore(LocalDateTime.now())) {
            return false;
        }
        
        // CORREGIDO: Encriptar la contraseña antes de guardarla
        String contrasenaEncriptada = passwordEncoder.encode(nuevaContrasena);
        usuario.setPassword(contrasenaEncriptada);
        
        // Limpiar token usado
        usuario.setTokenRecuperacion(null);
        usuario.setFechaExpiracionToken(null);
        
        usuarioRepositorio.save(usuario);
        
        return true;
    }
    
    private String crearCuerpoEmailRecuperacion(String username, String enlace) {
        // Usar String.format en lugar de .formatted() para mayor control
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                <div style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding: 30px; text-align: center;">
                    <h1 style="color: white; margin: 0;">Game Stats</h1>
                </div>
                
                <div style="padding: 30px; background: #f9f9f9;">
                    <h2 style="color: #333;">Hola %s,</h2>
                    
                    <p style="color: #666; font-size: 16px; line-height: 1.5;">
                        Recibimos una solicitud para restablecer la contraseña de tu cuenta.
                    </p>
                    
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s" style="
                            background: linear-gradient(135deg, #667eea, #764ba2);
                            color: white;
                            padding: 15px 30px;
                            text-decoration: none;
                            border-radius: 25px;
                            font-weight: bold;
                            display: inline-block;
                        ">Restablecer Contraseña</a>
                    </div>
                    
                    <p style="color: #666; font-size: 14px;">
                        Si no puedes hacer clic en el botón, copia y pega este enlace en tu navegador:
                    </p>
                    
                    
                    <p style="color: #999; font-size: 12px; margin-top: 30px;">
                        Este enlace expirará en 1 hora por seguridad.<br>
                        Si no solicitaste este cambio, puedes ignorar este email.
                    </p>
                </div>
            </body>
            </html>
            """, username, enlace);
    }
}