package com.ejemplos.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ejemplos.DTO.LoginRequest;
import com.ejemplos.DTO.LoginResponse;
import com.ejemplos.DTO.RecuperarContrasenaRequest;
import com.ejemplos.DTO.RegistroRequest;
import com.ejemplos.DTO.RestablecerContrasenaRequest;
import com.ejemplos.DTO.UsuarioRankingDTO;
import com.ejemplos.DTO.VerificarEmailRequest;
import com.ejemplos.configuracion.JwtUtil;
import com.ejemplos.modelo.Partida;
import com.ejemplos.modelo.PartidaRepositorio;
import com.ejemplos.modelo.Usuario;
import com.ejemplos.modelo.UsuarioRepositorio;
import com.ejemplos.servicios.EmailService;
import com.ejemplos.servicios.VerificacionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final UsuarioRepositorio usuarioRepositorio;
    private final JwtUtil jwtUtil;
    private final VerificacionService verificacionService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder; // AGREGADO: Para encriptar contraseñas

    
    	@Autowired
    	PartidaRepositorio partidaRepositorio;
    	
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Usuario user = usuarioRepositorio.findByEmail(request.getEmail());
        
     // CORREGIDO: Usar passwordEncoder para verificar contraseñas
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciales incorrectas");
        }
        
        if (!user.isEmailVerificado()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Email no verificado");
        }
        
        String token = jwtUtil.generateToken(user.getEmail());
        LoginResponse respuesta = new LoginResponse(token, user);
        return ResponseEntity.ok(respuesta);
    }
    
    @PostMapping("/registro")
    public ResponseEntity<?> registro(@RequestBody RegistroRequest request) {
        // Verificar si el usuario ya existe
        Usuario existente = usuarioRepositorio.findByEmail(request.getEmail());
        if (existente != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("El email ya está registrado");
        }
        
        // Crear nuevo usuario
        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setEmail(request.getEmail());
        String contrasenaEncriptada = passwordEncoder.encode(request.getPassword());
        nuevoUsuario.setPassword(contrasenaEncriptada);
        nuevoUsuario.setUsername(request.getUsername());
        nuevoUsuario.setEmailVerificado(false);
        
        // Guardar en la base de datos
        usuarioRepositorio.save(nuevoUsuario);
        
        // Enviar código de verificación
        verificacionService.enviarCodigoVerificacion(nuevoUsuario);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Usuario registrado. Revisa tu email para verificar tu cuenta.");
    }
    
    @PostMapping("/verificar-email")
    public ResponseEntity<?> verificarEmail(@RequestBody VerificarEmailRequest request) {
        boolean verificado = verificacionService.verificarCodigo(request.getCodigo());
        
        if (verificado) {
            return ResponseEntity.ok("Email verificado correctamente");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Código inválido o expirado");
        }
    }
    
    @PostMapping("/reenviar-codigo")
    public ResponseEntity<?> reenviarCodigo(@RequestBody RecuperarContrasenaRequest request) {
        Usuario usuario = usuarioRepositorio.findByEmail(request.getEmail());
        
        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado");
        }
        
        if (usuario.isEmailVerificado()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email ya verificado");
        }
        
        verificacionService.enviarCodigoVerificacion(usuario);
        return ResponseEntity.ok("Código de verificación reenviado");
    }
    
    // PASO 1: Usuario solicita recuperar contraseña (solo necesita email)
    @PostMapping("/solicitar-recuperacion")
    public ResponseEntity<?> solicitarRecuperacion(@RequestBody RecuperarContrasenaRequest request) {
        boolean enviado = verificacionService.enviarTokenRecuperacion(request.getEmail());
        
        if (enviado) {
            return ResponseEntity.ok("Se ha enviado un enlace de recuperación a tu email");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Usuario no encontrado o email no verificado");
        }
    }
    
    // PASO 2: Usuario hace clic en el enlace del email y llega aquí para validar el token
    @GetMapping("/validar-token-recuperacion")
    public ResponseEntity<?> validarTokenRecuperacion(@RequestParam String token) {
        boolean valido = verificacionService.validarTokenRecuperacion(token);
        
        if (valido) {
            return ResponseEntity.ok("Token válido. Puedes restablecer tu contraseña.");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Token inválido o expirado");
        }
    }
    
    // PASO 3: Usuario establece nueva contraseña usando el token
    @PostMapping("/restablecer-contrasena")
    public ResponseEntity<?> restablecerContrasena(@RequestBody RestablecerContrasenaRequest request) {
    	// AGREGADO: Validar que la nueva contraseña no esté vacía
        if (request.getNuevaContrasena() == null || request.getNuevaContrasena().trim().length() < 6) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("La contraseña debe tener al menos 6 caracteres");
        }
        
        boolean restablecido = verificacionService.restablecerContrasena(
                request.getToken(), request.getNuevaContrasena());
        
        if (restablecido) {
            return ResponseEntity.ok("Contraseña restablecida correctamente");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Token inválido o expirado");
        }
    }
    
    @GetMapping("/test-email")
    public String testEmail() {
        emailService.enviarCodigoVerificacion("thesensation005@gmail.com", "<html>\n"
        		+ "            <body style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;\">\n"
        		+ "                <div style=\"background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 30px; text-align: center;\">\n"
        		+ "                    <h1 style=\"color: white; margin: 0;\">Game Stats</h1>\n"
        		+ "                </div>\n"
        		+ "                \n"
        		+ "                <div style=\"padding: 30px; background: #f9f9f9;\">\n"
        		+ "                    <h2 style=\"color: #333;\">Hola %s,</h2>\n"
        		+ "                    \n"
        		+ "                    <p style=\"color: #666; font-size: 16px; line-height: 1.5;\">\n"
        		+ "                        Recibimos una solicitud para restablecer la contraseña de tu cuenta.\n"
        		+ "                    </p>\n"
        		+ "                    \n"
        		+ "                    <div style=\"text-align: center; margin: 30px 0;\">\n"
        		+ "                        <a href=\"%s\" style=\"\n"
        		+ "                            background: linear-gradient(135deg, #667eea, #764ba2);\n"
        		+ "                            color: white;\n"
        		+ "                            padding: 15px 30px;\n"
        		+ "                            text-decoration: none;\n"
        		+ "                            border-radius: 25px;\n"
        		+ "                            font-weight: bold;\n"
        		+ "                            display: inline-block;\n"
        		+ "                        \">Restablecer Contraseña</a>\n"
        		+ "                    </div>\n"
        		+ "                    \n"
        		+ "                    <p style=\"color: #666; font-size: 14px;\">\n"
        		+ "                        Si no puedes hacer clic en el botón, copia y pega este enlace en tu navegador:\n"
        		+ "                    </p>\n"
        		+ "                    <p style=\"color: #667eea; font-size: 14px; word-break: break-all;\">\n"
        		+ "                        %s\n"
        		+ "                    </p>\n"
        		+ "                    \n"
        		+ "                    <p style=\"color: #999; font-size: 12px; margin-top: 30px;\">\n"
        		+ "                        Este enlace expirará en 1 hora por seguridad.<br>\n"
        		+ "                        Si no solicitaste este cambio, puedes ignorar este email.\n"
        		+ "                    </p>\n"
        		+ "                </div>\n"
        		+ "            </body>\n"
        		+ "            </html>");
        return "Email enviado!";
    }
    
    
    @GetMapping("/usuario/ranking")
	public ResponseEntity<?> obtenerRankingUsuarios() {
	    List<Usuario> usuarios = usuarioRepositorio.findAll();
	    
	    List<UsuarioRankingDTO> ranking = usuarios.stream()
	            .map(usuario -> {
	                List<Partida> partidas = partidaRepositorio.findByUsuarioOrderByFechaDesc(usuario);
	                
	                // Añadir debug para ver los valores de fotoPerfil
	                System.out.println("Usuario: " + usuario.getUsername() + 
	                                   ", Foto: " + (usuario.getFotoPerfil() != null ? 
	                                    (usuario.getFotoPerfil().length() > 20 ? 
	                                     usuario.getFotoPerfil().substring(0, 20) + "..." : 
	                                     usuario.getFotoPerfil()) : 
	                                    "null"));
	                
	                return partidas.stream()
	                        .max((p1, p2) -> Integer.compare(p1.getPuntos(), p2.getPuntos()))
	                        .map(partidaMaxima -> new UsuarioRankingDTO(
	                                usuario.getUsername(),
	                                partidaMaxima.getPuntos(),
	                                partidaMaxima.getFecha(),
	                                usuario.getFotoPerfil() // AQUÍ ESTÁ EL CAMBIO: añadir el cuarto parámetro
	                                ))
	                        .orElse(new UsuarioRankingDTO(
	                                usuario.getUsername(),
	                                0,
	                                null,
	                                usuario.getFotoPerfil() // AQUÍ ESTÁ EL CAMBIO: añadir el cuarto parámetro
	                                ));
	            })
	            .sorted((r1, r2) -> Integer.compare(r2.getPuntosMaximos(), r1.getPuntosMaximos())) // orden descendente
	            .collect(Collectors.toList());
	    
	    // Agregar logs para verificar el contenido de las fotos de perfil en el ranking
	    for (int i = 0; i < Math.min(ranking.size(), 3); i++) { // Solo mostrar los primeros 3 para no saturar logs
	        UsuarioRankingDTO dto = ranking.get(i);
	        System.out.println("Ranking[" + i + "]: " + dto.getUsername() + 
	                           ", Foto: " + (dto.getFotoPerfil() != null ? 
	                            (dto.getFotoPerfil().length() > 20 ? 
	                             dto.getFotoPerfil().substring(0, 20) + "..." : 
	                             dto.getFotoPerfil()) : 
	                            "null"));
	    }
	    
	    return ResponseEntity.ok(ranking);
	}
}