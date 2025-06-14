package com.ejemplos.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import com.ejemplos.DTO.*;
import com.ejemplos.servicios.ChatService;
import com.ejemplos.modelo.Usuario;
import com.ejemplos.modelo.UsuarioRepositorio;
import java.util.List;

@Controller
public class ChatWebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private ChatService chatService;
    
    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    // Enviar mensaje en tiempo real
    @MessageMapping("/chat.enviarMensaje")
    public void enviarMensaje(@Payload EnviarMensajeDTO mensajeDTO, SimpMessageHeaderAccessor headerAccessor) {
        try {
            System.out.println("=== Enviando Mensaje WebSocket ===");
            System.out.println("Chat ID: " + mensajeDTO.getChatId());
            System.out.println("Usuario ID: " + mensajeDTO.getUsuarioId());
            System.out.println("Contenido: " + mensajeDTO.getContenido());
            
            // Verificar autenticación
            Authentication auth = (Authentication) headerAccessor.getUser();
            if (auth == null) {
                enviarError("No hay autenticación válida", headerAccessor.getSessionId());
                return;
            }
            
            String username = auth.getName();
            System.out.println("Usuario autenticado: " + username);
            
            // Verificar propiedad del usuario
            Usuario usuario = usuarioRepositorio.findById(mensajeDTO.getUsuarioId()).orElse(null);
            if (usuario == null || !usuario.getEmail().equals(username)) {
                enviarError("Usuario no autorizado para enviar mensaje", headerAccessor.getSessionId());
                return;
            }
            
            // Enviar mensaje usando el servicio
            MensajeDTO nuevoMensaje = chatService.enviarMensaje(mensajeDTO);
            
            // Obtener todos los participantes del chat para notificarles
            List<Long> participantesIds = chatService.obtenerParticipantesDelChat(mensajeDTO.getChatId());
            
            // Broadcast del nuevo mensaje a todos los participantes del chat
            messagingTemplate.convertAndSend("/topic/chat." + mensajeDTO.getChatId() + ".mensajes", nuevoMensaje);
            
            // Notificar a cada participante individualmente sobre el nuevo mensaje
            for (Long participanteId : participantesIds) {
                if (!participanteId.equals(mensajeDTO.getUsuarioId())) { // No notificar al remitente
                    NotificacionMensajeDTO notificacion = NotificacionMensajeDTO.builder()
                        .chatId(mensajeDTO.getChatId())
                        .remitenteId(mensajeDTO.getUsuarioId())
                        .remitenteNombre(usuario.getUsername())
                        .contenidoPreview(truncarContenido(mensajeDTO.getContenido()))
                        .tipoMensaje(mensajeDTO.getTipo())
                        .fechaEnvio(nuevoMensaje.getFechaEnvio())
                        .build();
                    
                    messagingTemplate.convertAndSend("/queue/user." + participanteId + ".notificaciones", notificacion);
                }
            }
            
            System.out.println("✅ Mensaje enviado exitosamente");
            
        } catch (Exception e) {
            System.err.println("❌ Error enviando mensaje: " + e.getMessage());
            e.printStackTrace();
            enviarError("Error al enviar mensaje: " + e.getMessage(), headerAccessor.getSessionId());
        }
    }

    // Marcar mensajes como leídos en tiempo real
    @MessageMapping("/chat.marcarLeido")
    public void marcarMensajesLeidos(@Payload MarcarLeidoDTO marcarLeidoDTO, SimpMessageHeaderAccessor headerAccessor) {
        try {
            System.out.println("=== Marcando Mensajes como Leídos ===");
            System.out.println("Chat ID: " + marcarLeidoDTO.getChatId());
            System.out.println("Usuario ID: " + marcarLeidoDTO.getUsuarioId());
            
            // Verificar autenticación
            Authentication auth = (Authentication) headerAccessor.getUser();
            if (auth == null) {
                enviarError("No hay autenticación válida", headerAccessor.getSessionId());
                return;
            }
            
            String username = auth.getName();
            
            // Verificar propiedad del usuario
            Usuario usuario = usuarioRepositorio.findById(marcarLeidoDTO.getUsuarioId()).orElse(null);
            if (usuario == null || !usuario.getEmail().equals(username)) {
                enviarError("Usuario no autorizado para marcar mensajes", headerAccessor.getSessionId());
                return;
            }
            
            // Marcar mensajes como leídos
            List<Long> mensajesMarcados = chatService.marcarMensajesComoLeidos(marcarLeidoDTO.getChatId(), marcarLeidoDTO.getUsuarioId());
            
            if (!mensajesMarcados.isEmpty()) {
                // Crear notificación de lectura
                NotificacionLecturaDTO notificacionLectura = NotificacionLecturaDTO.builder()
                    .chatId(marcarLeidoDTO.getChatId())
                    .usuarioId(marcarLeidoDTO.getUsuarioId())
                    .usuarioNombre(usuario.getUsername())
                    .mensajesLeidosIds(mensajesMarcados)
                    .fechaMarcado(java.time.LocalDateTime.now())
                    .build();
                
                // Notificar a todos los participantes del chat sobre el estado de lectura
                messagingTemplate.convertAndSend("/topic/chat." + marcarLeidoDTO.getChatId() + ".lectura", notificacionLectura);
                
                System.out.println("✅ Mensajes marcados como leídos: " + mensajesMarcados.size());
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error marcando mensajes como leídos: " + e.getMessage());
            e.printStackTrace();
            enviarError("Error al marcar mensajes como leídos: " + e.getMessage(), headerAccessor.getSessionId());
        }
    }

    // Obtener chats del usuario
    @MessageMapping("/chat.obtenerChats")
    public void obtenerChatsDeUsuario(@Payload ObtenerChatsDTO obtenerChatsDTO, SimpMessageHeaderAccessor headerAccessor) {
        try {
            System.out.println("=== Obteniendo Chats del Usuario ===");
            System.out.println("Usuario ID: " + obtenerChatsDTO.getUsuarioId());
            
            // Verificar autenticación
            Authentication auth = (Authentication) headerAccessor.getUser();
            if (auth == null) {
                enviarError("No hay autenticación válida", headerAccessor.getSessionId());
                return;
            }
            
            String username = auth.getName();
            
            // Verificar propiedad del usuario
            Usuario usuario = usuarioRepositorio.findById(obtenerChatsDTO.getUsuarioId()).orElse(null);
            if (usuario == null || !usuario.getEmail().equals(username)) {
                enviarError("Usuario no autorizado", headerAccessor.getSessionId());
                return;
            }
            
            // Obtener chats del usuario
            List<ChatDTO> chats = chatService.obtenerChatsDeUsuario(obtenerChatsDTO.getUsuarioId());
            
            // Enviar respuesta al usuario específico
            messagingTemplate.convertAndSendToUser(
                username, 
                "/queue/chats", 
                chats
            );
            
            System.out.println("✅ Chats obtenidos exitosamente: " + chats.size());
            
        } catch (Exception e) {
            System.err.println("❌ Error obteniendo chats: " + e.getMessage());
            e.printStackTrace();
            enviarError("Error al obtener chats: " + e.getMessage(), headerAccessor.getSessionId());
        }
    }

    // Obtener mensajes de un chat
    @MessageMapping("/chat.obtenerMensajes")
    public void obtenerMensajesDeChat(@Payload ObtenerMensajesDTO obtenerMensajesDTO, SimpMessageHeaderAccessor headerAccessor) {
        try {
            System.out.println("=== Obteniendo Mensajes del Chat ===");
            System.out.println("Chat ID: " + obtenerMensajesDTO.getChatId());
            System.out.println("Usuario ID: " + obtenerMensajesDTO.getUsuarioId());
            
            // Verificar autenticación
            Authentication auth = (Authentication) headerAccessor.getUser();
            if (auth == null) {
                enviarError("No hay autenticación válida", headerAccessor.getSessionId());
                return;
            }
            
            String username = auth.getName();
            
            // Verificar propiedad del usuario
            Usuario usuario = usuarioRepositorio.findById(obtenerMensajesDTO.getUsuarioId()).orElse(null);
            if (usuario == null || !usuario.getEmail().equals(username)) {
                enviarError("Usuario no autorizado", headerAccessor.getSessionId());
                return;
            }
            
            // Verificar que el usuario es participante del chat
            if (!chatService.esParticipanteDelChat(obtenerMensajesDTO.getChatId(), obtenerMensajesDTO.getUsuarioId())) {
                enviarError("Usuario no es participante del chat", headerAccessor.getSessionId());
                return;
            }
            
            // Obtener mensajes del chat
            List<MensajeDTO> mensajes = chatService.obtenerMensajesDeChat(obtenerMensajesDTO.getChatId());
            
            // Enviar respuesta al usuario específico
            messagingTemplate.convertAndSendToUser(
                username, 
                "/queue/chat." + obtenerMensajesDTO.getChatId() + ".mensajes.historial", 
                mensajes
            );
            
            System.out.println("✅ Mensajes obtenidos exitosamente: " + mensajes.size());
            
        } catch (Exception e) {
            System.err.println("❌ Error obteniendo mensajes: " + e.getMessage());
            e.printStackTrace();
            enviarError("Error al obtener mensajes: " + e.getMessage(), headerAccessor.getSessionId());
        }
    }

    // Crear chat privado
    @MessageMapping("/chat.crearPrivado")
    public void crearChatPrivado(@Payload CreateChatPrivadoDTO createChatDTO, SimpMessageHeaderAccessor headerAccessor) {
        try {
            System.out.println("=== Creando Chat Privado ===");
            System.out.println("Usuario 1 ID: " + createChatDTO.getUsuario1Id());
            System.out.println("Usuario 2 ID: " + createChatDTO.getUsuario2Id());
            
            // Verificar autenticación
            Authentication auth = (Authentication) headerAccessor.getUser();
            if (auth == null) {
                enviarError("No hay autenticación válida", headerAccessor.getSessionId());
                return;
            }
            
            String username = auth.getName();
            
            // Verificar que el usuario autenticado es uno de los participantes
            Usuario usuarioAuth = usuarioRepositorio.findByEmail(username);
            if (usuarioAuth == null || 
                (!usuarioAuth.getUsuarioId().equals(createChatDTO.getUsuario1Id()) && 
                 !usuarioAuth.getUsuarioId().equals(createChatDTO.getUsuario2Id()))) {
                enviarError("Usuario no autorizado para crear este chat", headerAccessor.getSessionId());
                return;
            }
            
            // Crear chat privado
            ChatDTO nuevoChat = chatService.crearChatPrivado(createChatDTO.getUsuario1Id(), createChatDTO.getUsuario2Id());
            
            // Notificar a ambos usuarios sobre el nuevo chat
            Usuario usuario1 = usuarioRepositorio.findById(createChatDTO.getUsuario1Id()).orElse(null);
            Usuario usuario2 = usuarioRepositorio.findById(createChatDTO.getUsuario2Id()).orElse(null);
            
            if (usuario1 != null) {
                messagingTemplate.convertAndSendToUser(
                    usuario1.getEmail(), 
                    "/queue/chat.nuevo", 
                    nuevoChat
                );
            }
            
            if (usuario2 != null) {
                messagingTemplate.convertAndSendToUser(
                    usuario2.getEmail(), 
                    "/queue/chat.nuevo", 
                    nuevoChat
                );
            }
            
            System.out.println("✅ Chat privado creado exitosamente: " + nuevoChat.getChatId());
            
        } catch (Exception e) {
            System.err.println("❌ Error creando chat privado: " + e.getMessage());
            e.printStackTrace();
            enviarError("Error al crear chat privado: " + e.getMessage(), headerAccessor.getSessionId());
        }
    }

    // NUEVO: Crear chat unificado (privado o grupal) usando CreateChatDTO
    @MessageMapping("/chat.crear")
    public void crearChat(@Payload CreateChatDTO createChatDTO, SimpMessageHeaderAccessor headerAccessor) {
        try {
            System.out.println("=== Creando Chat Unificado ===");
            System.out.println("Tipo: " + createChatDTO.getTipo());
            System.out.println("Participantes: " + createChatDTO.getParticipantesIds());
            
            // Verificar autenticación
            Authentication auth = (Authentication) headerAccessor.getUser();
            if (auth == null) {
                enviarError("No hay autenticación válida", headerAccessor.getSessionId());
                return;
            }
            
            String username = auth.getName();
            
            // Verificar que el usuario autenticado está en la lista de participantes
            Usuario usuarioAuth = usuarioRepositorio.findByEmail(username);
            if (usuarioAuth == null || !createChatDTO.getParticipantesIds().contains(usuarioAuth.getUsuarioId())) {
                enviarError("Usuario no autorizado para crear este chat", headerAccessor.getSessionId());
                return;
            }
            
            // Crear chat usando el servicio unificado
            ChatDTO nuevoChat = chatService.crearChat(createChatDTO, usuarioAuth.getUsuarioId());
            
            // Obtener todos los participantes del nuevo chat
            List<Long> participantesIds = chatService.obtenerParticipantesDelChat(nuevoChat.getChatId());
            
            // Notificar a todos los participantes sobre el nuevo chat
            for (Long participanteId : participantesIds) {
                Usuario participante = usuarioRepositorio.findById(participanteId).orElse(null);
                if (participante != null) {
                    messagingTemplate.convertAndSendToUser(
                        participante.getEmail(), 
                        "/queue/chat.nuevo", 
                        nuevoChat
                    );
                }
            }
            
            System.out.println("✅ Chat creado exitosamente: " + nuevoChat.getChatId() + " (Tipo: " + createChatDTO.getTipo() + ")");
            
        } catch (Exception e) {
            System.err.println("❌ Error creando chat: " + e.getMessage());
            e.printStackTrace();
            enviarError("Error al crear chat: " + e.getMessage(), headerAccessor.getSessionId());
        }
    }

    // Suscribirse a un chat (cuando el usuario entra a ver el chat)
    @MessageMapping("/chat.suscribir")
    public void suscribirseAChat(@Payload SuscribirChatDTO suscribirDTO, SimpMessageHeaderAccessor headerAccessor) {
        try {
            System.out.println("=== Usuario suscribiéndose al chat ===");
            System.out.println("Chat ID: " + suscribirDTO.getChatId());
            System.out.println("Usuario ID: " + suscribirDTO.getUsuarioId());
            
            // Verificar autenticación y autorización
            Authentication auth = (Authentication) headerAccessor.getUser();
            if (auth == null) {
                return;
            }
            
            String username = auth.getName();
            Usuario usuario = usuarioRepositorio.findById(suscribirDTO.getUsuarioId()).orElse(null);
            if (usuario == null || !usuario.getEmail().equals(username)) {
                return;
            }
            
            // Verificar que es participante del chat
            if (!chatService.esParticipanteDelChat(suscribirDTO.getChatId(), suscribirDTO.getUsuarioId())) {
                return;
            }
            
            // Marcar mensajes como leídos automáticamente al suscribirse
            chatService.marcarMensajesComoLeidos(suscribirDTO.getChatId(), suscribirDTO.getUsuarioId());
            
            // Notificar a otros participantes que este usuario está activo en el chat
            NotificacionPresenciaDTO presencia = NotificacionPresenciaDTO.builder()
                .chatId(suscribirDTO.getChatId())
                .usuarioId(suscribirDTO.getUsuarioId())
                .usuarioNombre(usuario.getUsername())
                .activo(true)
                .build();
            
            messagingTemplate.convertAndSend("/topic/chat." + suscribirDTO.getChatId() + ".presencia", presencia);
            
            System.out.println("✅ Usuario suscrito al chat exitosamente");
            
        } catch (Exception e) {
            System.err.println("❌ Error suscribiendo usuario al chat: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Métodos auxiliares
    private void enviarError(String mensaje, String sessionId) {
        ErrorDTO error = ErrorDTO.builder()
            .mensaje(mensaje)
            .timestamp(java.time.LocalDateTime.now())
            .build();
        
        messagingTemplate.convertAndSend("/queue/errors." + sessionId, error);
    }
    
    private String truncarContenido(String contenido) {
        if (contenido == null) return "";
        return contenido.length() > 50 ? contenido.substring(0, 50) + "..." : contenido;
    }
}