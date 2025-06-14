package com.ejemplos.servicios;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {
    
    private final JavaMailSender mailSender;
    
    public void enviarCodigoVerificacion(String email, String codigo) {
        String asunto = "Verificación de cuenta - Game Stats";
        String cuerpoEmail = crearCuerpoEmailVerificacion(codigo);
        
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            
            helper.setTo(email);
            helper.setSubject(asunto);
            helper.setText(cuerpoEmail, true); // true indica que es HTML
            
            mailSender.send(mensaje);
        } catch (MessagingException e) {
            // Fallback a SimpleMailMessage si falla el HTML
            SimpleMailMessage mensajeSimple = new SimpleMailMessage();
            mensajeSimple.setTo(email);
            mensajeSimple.setSubject(asunto);
            mensajeSimple.setText("Tu código de verificación es: " + codigo + 
                               "\nEste código expira en 24 horas.");
            mailSender.send(mensajeSimple);
        }
    }
    
    public void enviarTokenRecuperacion(String email, String token) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(email);
        mensaje.setSubject("Recuperación de contraseña");
        mensaje.setText(token);
        mailSender.send(mensaje);
    }
    public void enviarEmailRecuperacion(String email, String asunto, String cuerpoEmail) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            
            helper.setTo(email);
            helper.setSubject(asunto);
            helper.setText(cuerpoEmail, true); // true indica que es HTML
            
            mailSender.send(mensaje);
        } catch (MessagingException e) {
            // Fallback a SimpleMailMessage si falla el HTML
            SimpleMailMessage mensajeSimple = new SimpleMailMessage();
            mensajeSimple.setTo(email);
            mensajeSimple.setSubject(asunto);
            // Remover tags HTML básicamente para texto plano
            String textoPlano = cuerpoEmail.replaceAll("<[^>]*>", "").replaceAll("\\s+", " ");
            mensajeSimple.setText(textoPlano);
            mailSender.send(mensajeSimple);
        }
    }
    private String crearCuerpoEmailVerificacion(String codigo) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                <div style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding: 30px; text-align: center;">
                    <h1 style="color: white; margin: 0;">Game Stats</h1>
                </div>
                
                <div style="padding: 30px; background: #f9f9f9;">
                    <h2 style="color: #333;">¡Bienvenido!</h2>
                    
                    <p style="color: #666; font-size: 16px; line-height: 1.5;">
                        Para completar el registro de tu cuenta, necesitamos verificar tu dirección de email.
                    </p>
                    
                    <div style="text-align: center; margin: 30px 0;">
                        <div style="
                            background: linear-gradient(135deg, #667eea, #764ba2);
                            color: white;
                            padding: 20px 40px;
                            border-radius: 15px;
                            font-weight: bold;
                            font-size: 32px;
                            letter-spacing: 8px;
                            display: inline-block;
                            box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
                        ">%s</div>
                    </div>
                    
                    <p style="color: #666; font-size: 16px; text-align: center;">
                        Introduce este código en la app para verificar tu cuenta.
                    </p>
                    
                    <div style="background: #fff; padding: 20px; border-radius: 10px; margin: 20px 0; border-left: 4px solid #667eea;">
                        <p style="color: #666; font-size: 14px; margin: 0;">
                            <strong>Importante:</strong> Este código expira en 24 horas por seguridad.
                        </p>
                    </div>
                    
                    <p style="color: #999; font-size: 12px; margin-top: 30px;">
                        Si no te registraste en Game Stats, puedes ignorar este email.<br>
                        Tu cuenta no será creada sin la verificación.
                    </p>
                </div>
            </body>
            </html>
            """, codigo);
    }
}