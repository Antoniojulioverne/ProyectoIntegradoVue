package com.ejemplos.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
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
    
    // Nuevo endpoint para obtener mensajes no leídos
    @GetMapping("/usuario/{usuarioId}/no-leidos")
    public ResponseEntity<List<MensajeDTO>> obtenerMensajesNoLeidos(@PathVariable Long usuarioId) {
        List<MensajeDTO> mensajesNoLeidos = chatService.obtenerMensajesNoLeidos(usuarioId);
        return ResponseEntity.ok(mensajesNoLeidos);
    }
    
    // Nuevo endpoint para contar mensajes no leídos en un chat específico
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
    
    // Endpoint de respaldo para crear chat (principalmente para testing)
    @PostMapping("/privado")
    public ResponseEntity<ChatDTO> crearChatPrivado(@RequestBody CreateChatPrivadoDTO dto) {
        ChatDTO chat = chatService.crearChatPrivado(dto.getUsuario1Id(), dto.getUsuario2Id());
        return ResponseEntity.ok(chat);
    }
}