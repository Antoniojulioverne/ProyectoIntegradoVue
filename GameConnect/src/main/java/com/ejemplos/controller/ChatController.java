package com.ejemplos.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.ejemplos.DTO.*;
import com.ejemplos.servicios.ChatService;

@RestController
@RequestMapping("/GameConnect/chat")
public class ChatController {
    
    @Autowired
    private ChatService chatService;
    
    // Endpoints de respaldo para cuando WebSocket no esté disponible
    // o para operaciones de administración
    
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<ChatDTO>> obtenerChatsDeUsuario(@PathVariable Long usuarioId) {
        List<ChatDTO> chats = chatService.obtenerChatsDeUsuario(usuarioId);
        return ResponseEntity.ok(chats);
    }
    
    @GetMapping("/{chatId}/mensajes")
    public ResponseEntity<List<MensajeDTO>> obtenerMensajesDeChat(@PathVariable Long chatId) {
        List<MensajeDTO> mensajes = chatService.obtenerMensajesDeChat(chatId);
        return ResponseEntity.ok(mensajes);
    }
    
    // Endpoint para obtener mensajes no leídos
    @GetMapping("/usuario/{usuarioId}/no-leidos")
    public ResponseEntity<List<MensajeDTO>> obtenerMensajesNoLeidos(@PathVariable Long usuarioId) {
        List<MensajeDTO> mensajesNoLeidos = chatService.obtenerMensajesNoLeidos(usuarioId);
        return ResponseEntity.ok(mensajesNoLeidos);
    }
    
    // Endpoint para contar mensajes no leídos en un chat específico
    @GetMapping("/{chatId}/no-leidos/count")
    public ResponseEntity<Long> contarMensajesNoLeidos(
            @PathVariable Long chatId, 
            @RequestParam Long usuarioId) {
        Long count = chatService.contarMensajesNoLeidos(chatId, usuarioId);
        return ResponseEntity.ok(count);
    }
    
    // Endpoint para obtener información de un chat específico
    @GetMapping("/{chatId}")
    public ResponseEntity<ChatDTO> obtenerChat(
            @PathVariable Long chatId, 
            @RequestParam Long usuarioId) {
        ChatDTO chat = chatService.obtenerChatPorId(chatId, usuarioId);
        return ResponseEntity.ok(chat);
    }
    
    // NUEVO: Endpoint unificado para crear cualquier tipo de chat
    @PostMapping
    public ResponseEntity<?> crearChat(@RequestBody CreateChatDTO dto, @RequestParam Long creadorId) {
        try {
            ChatDTO nuevoChat = chatService.crearChat(dto, creadorId);
            return ResponseEntity.ok(nuevoChat);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error al crear chat: " + e.getMessage());
        }
    }
    
    // Endpoint de respaldo para crear chat privado (mantener compatibilidad)
    @PostMapping("/privado")
    public ResponseEntity<?> crearChatPrivado(@RequestBody CreateChatPrivadoDTO dto) {
        try {
            ChatDTO chat = chatService.crearChatPrivado(dto.getUsuario1Id(), dto.getUsuario2Id());
            return ResponseEntity.ok(chat);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error al crear chat privado: " + e.getMessage());
        }
    }
    
    // NUEVO: Endpoint específico para crear chat grupal
    @PostMapping("/grupo")
    public ResponseEntity<?> crearChatGrupal(
            @RequestBody CreateChatDTO dto, 
            @RequestParam Long creadorId) {
        try {
            // Validar que sea un grupo
            if (!"GRUPO".equals(dto.getTipo())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Este endpoint es solo para crear grupos");
            }
            
            ChatDTO nuevoChat = chatService.crearChatGrupal(
                dto.getNombreChat(), 
                dto.getParticipantesIds(), 
                creadorId
            );
            return ResponseEntity.ok(nuevoChat);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error al crear chat grupal: " + e.getMessage());
        }
    }
    
    // NUEVO: Endpoint para enviar mensajes (respaldo del WebSocket)
    @PostMapping("/{chatId}/mensajes")
    public ResponseEntity<?> enviarMensaje(
            @PathVariable Long chatId,
            @RequestBody EnviarMensajeDTO dto) {
        try {
            // Asegurar que el chatId sea correcto
            dto.setChatId(chatId);
            
            MensajeDTO nuevoMensaje = chatService.enviarMensaje(dto);
            return ResponseEntity.ok(nuevoMensaje);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error al enviar mensaje: " + e.getMessage());
        }
    }
    
    // NUEVO: Endpoint para marcar mensajes como leídos
    @PutMapping("/{chatId}/mensajes/marcar-leidos")
    public ResponseEntity<?> marcarMensajesLeidos(
            @PathVariable Long chatId,
            @RequestParam Long usuarioId) {
        try {
            List<Long> mensajesMarcados = chatService.marcarMensajesComoLeidos(chatId, usuarioId);
            return ResponseEntity.ok(mensajesMarcados);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error al marcar mensajes como leídos: " + e.getMessage());
        }
    }
    
    // NUEVO: Obtener participantes de un chat
    @GetMapping("/{chatId}/participantes")
    public ResponseEntity<List<Long>> obtenerParticipantes(@PathVariable Long chatId) {
        List<Long> participantes = chatService.obtenerParticipantesDelChat(chatId);
        return ResponseEntity.ok(participantes);
    }
    
    // NUEVO: Verificar si un usuario es participante de un chat
    @GetMapping("/{chatId}/participantes/{usuarioId}/verificar")
    public ResponseEntity<Boolean> verificarParticipante(
            @PathVariable Long chatId,
            @PathVariable Long usuarioId) {
        boolean esParticipante = chatService.esParticipanteDelChat(chatId, usuarioId);
        return ResponseEntity.ok(esParticipante);
    }
    
    // NUEVO: Agregar participante a grupo (solo para administradores)
    @PostMapping("/{chatId}/participantes")
    public ResponseEntity<?> agregarParticipante(
            @PathVariable Long chatId,
            @RequestParam Long usuarioId,
            @RequestParam Long adminId) {
        try {
            chatService.agregarParticipanteAGrupo(chatId, usuarioId, adminId);
            return ResponseEntity.ok("Participante agregado exitosamente");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error al agregar participante: " + e.getMessage());
        }
    }
    
    // NUEVO: Remover participante de grupo (solo para administradores)
    @DeleteMapping("/{chatId}/participantes/{usuarioId}")
    public ResponseEntity<?> removerParticipante(
            @PathVariable Long chatId,
            @PathVariable Long usuarioId,
            @RequestParam Long adminId) {
        try {
            chatService.removerParticipanteDeGrupo(chatId, usuarioId, adminId);
            return ResponseEntity.ok("Participante removido exitosamente");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error al remover participante: " + e.getMessage());
        }
    }
    
    // NUEVO: Obtener último mensaje de un chat
    @GetMapping("/{chatId}/ultimo-mensaje")
    public ResponseEntity<?> obtenerUltimoMensaje(@PathVariable Long chatId) {
        return chatService.obtenerUltimoMensaje(chatId)
            .map(mensaje -> ResponseEntity.ok(mensaje))
            .orElse(ResponseEntity.noContent().build());
    }
    
    
}