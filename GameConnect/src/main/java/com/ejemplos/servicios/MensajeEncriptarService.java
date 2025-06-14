// ===== 1. SERVICIO DE CIFRADO SIMPLE =====
// GameConnect/src/main/java/com/ejemplos/servicios/MessageEncryptionService.java
package com.ejemplos.servicios;

import org.springframework.stereotype.Service;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Service
public class MensajeEncriptarService {

    private static final String SECRET_KEY = "GameConnect2025!"; // 16 caracteres para AES-128
    private static final String ALGORITHM = "AES";

    /**
     * Cifra un mensaje
     */
    public String cifrarMensaje(String mensaje) {
        try {
            if (mensaje == null || mensaje.trim().isEmpty()) {
                return mensaje;
            }
            
            SecretKeySpec key = new SecretKeySpec(SECRET_KEY.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            
            byte[] encrypted = cipher.doFinal(mensaje.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
            
        } catch (Exception e) {
            System.err.println("Error cifrando mensaje: " + e.getMessage());
            return mensaje; // Si falla, devolver original
        }
    }

    /**
     * Descifra un mensaje
     */
    public String descifrarMensaje(String mensajeCifrado) {
        try {
            if (mensajeCifrado == null || mensajeCifrado.trim().isEmpty()) {
                return mensajeCifrado;
            }
            
            SecretKeySpec key = new SecretKeySpec(SECRET_KEY.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(mensajeCifrado));
            return new String(decrypted);
            
        } catch (Exception e) {
            System.err.println("Error descifrando mensaje: " + e.getMessage());
            return mensajeCifrado; // Si falla, devolver original
        }
    }
}