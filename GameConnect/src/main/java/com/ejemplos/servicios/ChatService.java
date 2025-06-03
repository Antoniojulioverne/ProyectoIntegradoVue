package com.ejemplos.servicios;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ejemplos.DTO.*;
import com.ejemplos.modelo.*;

@Service
@Transactional
public class ChatService {
    
    @Autowired
    private ChatRepositorio chatRepositorio;
    
    @Autowired
    private MensajeRepositorio mensajeRepositorio;
    
    @Autowired
    private ChatParticipanteRepositorio chatParticipanteRepositorio;
    
    @Autowired
    private UsuarioRepositorio usuarioRepositorio;
    
    @Autowired
    private ChatDTOConverter chatDTOConverter;
    
    @Autowired
    private MensajeDTOConverter mensajeDTOConverter;
    
    public ChatDTO crearChatPrivado(Long usuario1Id, Long usuario2Id) {
        // Verificar si ya existe un chat privado entre estos usuarios
        Optional<Chat> chatExistente = chatRepositorio.findChatPrivadoEntreUsuarios(usuario1Id, usuario2Id);
        if (chatExistente.isPresent()) {
            return chatDTOConverter.convertirADto(chatExistente.get(), usuario1Id);
        }
        
        // Crear nuevo chat privado
        Usuario usuario1 = usuarioRepositorio.findById(usuario1Id).orElseThrow(
            () -> new RuntimeException("Usuario 1 no encontrado")
        );
        Usuario usuario2 = usuarioRepositorio.findById(usuario2Id).orElseThrow(
            () -> new RuntimeException("Usuario 2 no encontrado")
        );
        
        Chat chat = new Chat();
        chat.setNombreChat(usuario1.getUsername() + " - " + usuario2.getUsername());
        chat.setTipo(TipoChat.PRIVADO);
        
        chat = chatRepositorio.save(chat);
        
        // Agregar participantes
        chatParticipanteRepositorio.save(ChatParticipante.builder()
            .chat(chat).usuario(usuario1).build());
        chatParticipanteRepositorio.save(ChatParticipante.builder()
            .chat(chat).usuario(usuario2).build());
        
        return chatDTOConverter.convertirADto(chat, usuario1Id);
    }
    
    // NUEVO: Método para crear chat usando el DTO existente CreateChatDTO
    public ChatDTO crearChat(CreateChatDTO dto, Long creadorId) {
        if (dto.getTipo() == null) {
            throw new RuntimeException("El tipo de chat es obligatorio");
        }
        
        if (dto.getTipo().equals("PRIVADO")) {
            // Para chat privado, necesitamos exactamente 2 participantes
            if (dto.getParticipantesIds() == null || dto.getParticipantesIds().size() != 2) {
                throw new RuntimeException("Chat privado requiere exactamente 2 participantes");
            }
            
            Long usuario1Id = creadorId;
            Long usuario2Id = dto.getParticipantesIds().stream()
                .filter(id -> !id.equals(creadorId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Segundo participante no válido"));
            
            return crearChatPrivado(usuario1Id, usuario2Id);
            
        } else if (dto.getTipo().equals("GRUPO")) {
            // Para chat grupal
            if (dto.getNombreChat() == null || dto.getNombreChat().trim().isEmpty()) {
                throw new RuntimeException("Se requiere nombre para chat grupal");
            }
            if (dto.getParticipantesIds() == null || dto.getParticipantesIds().isEmpty()) {
                throw new RuntimeException("Se requieren participantes para chat grupal");
            }
            
            return crearChatGrupal(dto.getNombreChat(), dto.getParticipantesIds(), creadorId);
            
        } else {
            throw new RuntimeException("Tipo de chat no soportado: " + dto.getTipo());
        }
    }
    
    // NUEVO: Método para crear chat grupal
    public ChatDTO crearChatGrupal(String nombreGrupo, List<Long> participantesIds, Long creadorId) {
        // Validar datos de entrada
        if (nombreGrupo == null || nombreGrupo.trim().isEmpty()) {
            throw new RuntimeException("El nombre del grupo es obligatorio");
        }
        
        if (participantesIds == null || participantesIds.isEmpty()) {
            throw new RuntimeException("El grupo debe tener al menos un participante");
        }
        
        // Verificar que el creador existe
        Usuario creador = usuarioRepositorio.findById(creadorId).orElseThrow(
            () -> new RuntimeException("Usuario creador no encontrado")
        );
        
        // Crear el chat grupal
        Chat chat = new Chat();
        chat.setNombreChat(nombreGrupo.trim());
        chat.setTipo(TipoChat.GRUPO);
        
        chat = chatRepositorio.save(chat);
        
        // Agregar el creador como administrador
        chatParticipanteRepositorio.save(ChatParticipante.builder()
            .chat(chat)
            .usuario(creador)
            .esAdmin(true)
            .build());
        
        // Agregar los demás participantes
        for (Long participanteId : participantesIds) {
            if (!participanteId.equals(creadorId)) { // No duplicar el creador
                Usuario participante = usuarioRepositorio.findById(participanteId).orElse(null);
                if (participante != null) {
                    chatParticipanteRepositorio.save(ChatParticipante.builder()
                        .chat(chat)
                        .usuario(participante)
                        .esAdmin(false)
                        .build());
                }
            }
        }
        
        return chatDTOConverter.convertirADto(chat, creadorId);
    }
    
    public List<ChatDTO> obtenerChatsDeUsuario(Long usuarioId) {
        List<Chat> chats = chatRepositorio.findChatsByUsuarioId(usuarioId);
        return chats.stream()
            .map(chat -> chatDTOConverter.convertirADto(chat, usuarioId))
            .collect(Collectors.toList());
    }
    
    public List<MensajeDTO> obtenerMensajesDeChat(Long chatId) {
        List<Mensaje> mensajes = mensajeRepositorio.findByChatChatIdOrderByFechaEnvioAsc(chatId);
        return mensajes.stream()
            .map(mensajeDTOConverter::convertirADto)
            .collect(Collectors.toList());
    }
    
    public MensajeDTO enviarMensaje(EnviarMensajeDTO dto) {
        Chat chat = chatRepositorio.findById(dto.getChatId()).orElseThrow(
            () -> new RuntimeException("Chat no encontrado")
        );
        Usuario usuario = usuarioRepositorio.findById(dto.getUsuarioId()).orElseThrow(
            () -> new RuntimeException("Usuario no encontrado")
        );
        
        // Verificar que el usuario es participante del chat
        if (!chatParticipanteRepositorio.existsByChatChatIdAndUsuarioUsuarioId(dto.getChatId(), dto.getUsuarioId())) {
            throw new RuntimeException("El usuario no es participante de este chat");
        }
        
        Mensaje mensaje = Mensaje.builder()
            .chat(chat)
            .usuario(usuario)
            .contenido(dto.getContenido())
            .tipo(TipoMensaje.valueOf(dto.getTipo()))
            .esLeido(false) // Inicializar como no leído
            .build();
        
        mensaje = mensajeRepositorio.save(mensaje);
        return mensajeDTOConverter.convertirADto(mensaje);
    }
    
    // Método actualizado que retorna los IDs de mensajes marcados como leídos
    public List<Long> marcarMensajesComoLeidos(Long chatId, Long usuarioId) {
        List<Mensaje> mensajes = mensajeRepositorio.findByChatChatIdOrderByFechaEnvioAsc(chatId);
        List<Long> mensajesMarcados = new java.util.ArrayList<>();
        
        mensajes.stream()
            .filter(m -> !m.getUsuario().getUsuarioId().equals(usuarioId) && 
                        (m.getEsLeido() == null || !m.getEsLeido())) // Manejar valores null
            .forEach(m -> {
                m.setEsLeido(true);
                mensajeRepositorio.save(m);
                mensajesMarcados.add(m.getMensajeId());
            });
        
        return mensajesMarcados;
    }
    
    // Nuevo método para obtener participantes de un chat
    public List<Long> obtenerParticipantesDelChat(Long chatId) {
        List<ChatParticipante> participantes = chatParticipanteRepositorio.findByChatChatId(chatId);
        return participantes.stream()
            .map(p -> p.getUsuario().getUsuarioId())
            .collect(Collectors.toList());
    }
    
    // Nuevo método para verificar si un usuario es participante de un chat
    public boolean esParticipanteDelChat(Long chatId, Long usuarioId) {
        return chatParticipanteRepositorio.existsByChatChatIdAndUsuarioUsuarioId(chatId, usuarioId);
    }
    
    // Nuevo método para obtener información de un chat específico
    public ChatDTO obtenerChatPorId(Long chatId, Long usuarioId) {
        Chat chat = chatRepositorio.findById(chatId).orElseThrow(
            () -> new RuntimeException("Chat no encontrado")
        );
        
        // Verificar que el usuario es participante
        if (!esParticipanteDelChat(chatId, usuarioId)) {
            throw new RuntimeException("El usuario no es participante de este chat");
        }
        
        return chatDTOConverter.convertirADto(chat, usuarioId);
    }
    
    // Nuevo método para obtener mensajes no leídos de un usuario
    public List<MensajeDTO> obtenerMensajesNoLeidos(Long usuarioId) {
        List<Chat> chatsDelUsuario = chatRepositorio.findChatsByUsuarioId(usuarioId);
        List<MensajeDTO> mensajesNoLeidos = new java.util.ArrayList<>();
        
        for (Chat chat : chatsDelUsuario) {
            List<Mensaje> mensajes = mensajeRepositorio.findByChatChatIdOrderByFechaEnvioAsc(chat.getChatId());
            mensajes.stream()
                .filter(m -> !m.getUsuario().getUsuarioId().equals(usuarioId) && 
                            (m.getEsLeido() == null || !m.getEsLeido()))
                .forEach(m -> mensajesNoLeidos.add(mensajeDTOConverter.convertirADto(m)));
        }
        
        return mensajesNoLeidos;
    }
    
    // Nuevo método para contar mensajes no leídos por chat
    public Long contarMensajesNoLeidos(Long chatId, Long usuarioId) {
        List<Mensaje> mensajes = mensajeRepositorio.findByChatChatIdOrderByFechaEnvioAsc(chatId);
        return mensajes.stream()
            .filter(m -> !m.getUsuario().getUsuarioId().equals(usuarioId) && 
                        (m.getEsLeido() == null || !m.getEsLeido()))
            .count();
    }
    
    // Nuevo método para obtener el último mensaje de un chat
    public Optional<MensajeDTO> obtenerUltimoMensaje(Long chatId) {
        List<Mensaje> mensajes = mensajeRepositorio.findByChatChatIdOrderByFechaEnvioDesc(chatId);
        if (!mensajes.isEmpty()) {
            return Optional.of(mensajeDTOConverter.convertirADto(mensajes.get(0)));
        }
        return Optional.empty();
    }
    
    // NUEVO: Métodos adicionales para gestión de grupos
    public void agregarParticipanteAGrupo(Long chatId, Long usuarioId, Long adminId) {
        Chat chat = chatRepositorio.findById(chatId).orElseThrow(
            () -> new RuntimeException("Chat no encontrado")
        );
        
        if (chat.getTipo() != TipoChat.GRUPO) {
            throw new RuntimeException("Solo se pueden agregar participantes a grupos");
        }
        
        // Verificar que quien agrega es admin
        ChatParticipante admin = chatParticipanteRepositorio
            .findByChatChatIdAndUsuarioUsuarioId(chatId, adminId)
            .orElseThrow(() -> new RuntimeException("Administrador no encontrado"));
        
        if (!admin.getEsAdmin()) {
            throw new RuntimeException("Solo los administradores pueden agregar participantes");
        }
        
        // Verificar que el usuario no es ya participante
        if (chatParticipanteRepositorio.existsByChatChatIdAndUsuarioUsuarioId(chatId, usuarioId)) {
            throw new RuntimeException("El usuario ya es participante del grupo");
        }
        
        Usuario usuario = usuarioRepositorio.findById(usuarioId).orElseThrow(
            () -> new RuntimeException("Usuario no encontrado")
        );
        
        chatParticipanteRepositorio.save(ChatParticipante.builder()
            .chat(chat)
            .usuario(usuario)
            .esAdmin(false)
            .build());
    }
    
    public void removerParticipanteDeGrupo(Long chatId, Long usuarioId, Long adminId) {
        Chat chat = chatRepositorio.findById(chatId).orElseThrow(
            () -> new RuntimeException("Chat no encontrado")
        );
        
        if (chat.getTipo() != TipoChat.GRUPO) {
            throw new RuntimeException("Solo se pueden remover participantes de grupos");
        }
        
        // Verificar que quien remueve es admin
        ChatParticipante admin = chatParticipanteRepositorio
            .findByChatChatIdAndUsuarioUsuarioId(chatId, adminId)
            .orElseThrow(() -> new RuntimeException("Administrador no encontrado"));
        
        if (!admin.getEsAdmin()) {
            throw new RuntimeException("Solo los administradores pueden remover participantes");
        }
        
        ChatParticipante participante = chatParticipanteRepositorio
            .findByChatChatIdAndUsuarioUsuarioId(chatId, usuarioId)
            .orElseThrow(() -> new RuntimeException("Participante no encontrado"));
        
        chatParticipanteRepositorio.delete(participante);
    }
}