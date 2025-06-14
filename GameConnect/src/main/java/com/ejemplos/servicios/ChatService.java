package com.ejemplos.servicios;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
	private MensajeEncriptarService mensajeEncriptarService;
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
            .map(mensaje -> {
                MensajeDTO dto = mensajeDTOConverter.convertirADto(mensaje);
                // *** ÚNICA LÍNEA AGREGADA ***
                dto.setContenido(mensajeEncriptarService.descifrarMensaje(mensaje.getContenido()));
                return dto;
            })
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
            .contenido(mensajeEncriptarService.cifrarMensaje(dto.getContenido()))
            .tipo(TipoMensaje.valueOf(dto.getTipo()))
            .esLeido(false) // Inicializar como no leído
            .build();
        
        mensaje = mensajeRepositorio.save(mensaje);
        // *** CORREGIR: Devolver contenido descifrado ***
        MensajeDTO mensajeDTO = mensajeDTOConverter.convertirADto(mensaje);
        mensajeDTO.setContenido(dto.getContenido()); // Contenido original (descifrado)
        return mensajeDTO;
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
                .forEach(m -> {
                    MensajeDTO dto = mensajeDTOConverter.convertirADto(m);
                    dto.setContenido(mensajeEncriptarService.descifrarMensaje(m.getContenido()));
                    mensajesNoLeidos.add(dto);
                });
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
    
    public Optional<MensajeDTO> obtenerUltimoMensaje(Long chatId) {
        List<Mensaje> mensajes = mensajeRepositorio.findByChatChatIdOrderByFechaEnvioDesc(chatId);
        if (!mensajes.isEmpty()) {
            MensajeDTO dto = mensajeDTOConverter.convertirADto(mensajes.get(0));
            // *** AGREGAR ESTA LÍNEA ***
            dto.setContenido(mensajeEncriptarService.descifrarMensaje(mensajes.get(0).getContenido()));
            return Optional.of(dto);
        }
        return Optional.empty();
    }
    
    /**
     * Método mejorado para agregar participante con validaciones adicionales
     */
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
        
        if (admin.getEsAdmin() == null || !admin.getEsAdmin()) {
            throw new RuntimeException("Solo los administradores pueden agregar participantes");
        }
        
        // Verificar que el usuario no es ya participante
        if (chatParticipanteRepositorio.existsByChatChatIdAndUsuarioUsuarioId(chatId, usuarioId)) {
            throw new RuntimeException("El usuario ya es participante del grupo");
        }
        
        Usuario usuario = usuarioRepositorio.findById(usuarioId).orElseThrow(
            () -> new RuntimeException("Usuario no encontrado")
        );
        
        // Verificar que el usuario esté verificado
        if (!usuario.isEmailVerificado()) {
            throw new RuntimeException("Solo usuarios verificados pueden ser agregados al grupo");
        }
        
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
        
        if (admin.getEsAdmin() == null || !admin.getEsAdmin()) {
            throw new RuntimeException("Solo los administradores pueden remover participantes");
        }
        
        // No permitir auto-remoción
        if (usuarioId.equals(adminId)) {
            throw new RuntimeException("No puedes removerte a ti mismo. Usa la función 'Salir del grupo'");
        }
        
        ChatParticipante participante = chatParticipanteRepositorio
            .findByChatChatIdAndUsuarioUsuarioId(chatId, usuarioId)
            .orElseThrow(() -> new RuntimeException("Participante no encontrado"));
        
        chatParticipanteRepositorio.delete(participante);
    }
    
    
    /**
     * Actualiza el nombre de un grupo
     */
    public void actualizarNombreGrupo(Long chatId, String nuevoNombre) {
        Chat chat = chatRepositorio.findById(chatId).orElseThrow(
            () -> new RuntimeException("Chat no encontrado")
        );
        
        if (chat.getTipo() != TipoChat.GRUPO) {
            throw new RuntimeException("Solo se puede cambiar el nombre de grupos");
        }
        
        if (nuevoNombre == null || nuevoNombre.trim().isEmpty()) {
            throw new RuntimeException("El nombre del grupo no puede estar vacío");
        }
        
        if (nuevoNombre.trim().length() > 50) {
            throw new RuntimeException("El nombre del grupo no puede exceder 50 caracteres");
        }
        
        chat.setNombreChat(nuevoNombre.trim());
        chatRepositorio.save(chat);
    }
    
    /**
     * Promueve un usuario a administrador
     */
    public void promoverAAdmin(Long chatId, Long usuarioId) {
        Chat chat = chatRepositorio.findById(chatId).orElseThrow(
            () -> new RuntimeException("Chat no encontrado")
        );
        
        if (chat.getTipo() != TipoChat.GRUPO) {
            throw new RuntimeException("Solo se pueden promover administradores en grupos");
        }
        
        ChatParticipante participante = chatParticipanteRepositorio
            .findByChatChatIdAndUsuarioUsuarioId(chatId, usuarioId)
            .orElseThrow(() -> new RuntimeException("El usuario no es participante del grupo"));
        
        if (participante.getEsAdmin()) {
            throw new RuntimeException("El usuario ya es administrador");
        }
        
        participante.setEsAdmin(true);
        chatParticipanteRepositorio.save(participante);
    }
    
    /**
     * Permite que un usuario salga del grupo por sí mismo
     */
    public void salirDelGrupo(Long chatId, Long usuarioId) {
        Chat chat = chatRepositorio.findById(chatId).orElseThrow(
            () -> new RuntimeException("Chat no encontrado")
        );
        
        if (chat.getTipo() != TipoChat.GRUPO) {
            throw new RuntimeException("Solo se puede salir de grupos");
        }
        
        ChatParticipante participante = chatParticipanteRepositorio
            .findByChatChatIdAndUsuarioUsuarioId(chatId, usuarioId)
            .orElseThrow(() -> new RuntimeException("El usuario no es participante del grupo"));
        
        // Verificar si es el último administrador
        long totalAdmins = chatParticipanteRepositorio.findByChatChatId(chatId).stream()
            .filter(p -> p.getEsAdmin() != null && p.getEsAdmin())
            .count();
        
        if (participante.getEsAdmin() && totalAdmins == 1) {
            // Si es el último admin, promover a otro usuario
            List<ChatParticipante> otrosParticipantes = chatParticipanteRepositorio.findByChatChatId(chatId)
                .stream()
                .filter(p -> !p.getUsuario().getUsuarioId().equals(usuarioId))
                .collect(Collectors.toList());
            
            if (!otrosParticipantes.isEmpty()) {
                // Promover al primer participante encontrado
                ChatParticipante nuevoAdmin = otrosParticipantes.get(0);
                nuevoAdmin.setEsAdmin(true);
                chatParticipanteRepositorio.save(nuevoAdmin);
            }
        }
        
        chatParticipanteRepositorio.delete(participante);
        
        // Si no quedan participantes, eliminar el chat
        long participantesRestantes = chatParticipanteRepositorio.findByChatChatId(chatId).size();
        if (participantesRestantes == 0) {
            chatRepositorio.delete(chat);
        }
    }
    
    /**
     * Obtiene la lista de IDs de usuarios que son administradores
     */
    public List<Long> obtenerAdministradores(Long chatId) {
        return chatParticipanteRepositorio.findByChatChatId(chatId).stream()
            .filter(p -> p.getEsAdmin() != null && p.getEsAdmin())
            .map(p -> p.getUsuario().getUsuarioId())
            .collect(Collectors.toList());
    }
    /**
     * Verifica si un usuario es administrador del chat
     */
    public boolean esAdministrador(Long chatId, Long usuarioId) {
        return chatParticipanteRepositorio
            .findByChatChatIdAndUsuarioUsuarioId(chatId, usuarioId)
            .map(p -> p.getEsAdmin() != null && p.getEsAdmin())
            .orElse(false);
    }
    
    /**
     * Obtiene participantes con información detallada incluyendo si son admin
     */
    public List<Map<String, Object>> obtenerParticipantesDetallado(Long chatId) {
        List<ChatParticipante> participantes = chatParticipanteRepositorio.findByChatChatId(chatId);
        
        return participantes.stream()
            .map(p -> {
                Map<String, Object> participanteInfo = new HashMap<>();
                participanteInfo.put("usuarioId", p.getUsuario().getUsuarioId());
                participanteInfo.put("username", p.getUsuario().getUsername());
                participanteInfo.put("email", p.getUsuario().getEmail());
                participanteInfo.put("skin", p.getUsuario().getSkin());
                participanteInfo.put("esAdmin", p.getEsAdmin() != null && p.getEsAdmin());
                participanteInfo.put("fechaUnion", p.getFechaUnion());
                return participanteInfo;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Obtiene información general del chat para debugging
     */
    public Map<String, Object> obtenerInfoGeneralChat(Long chatId) {
        Chat chat = chatRepositorio.findById(chatId).orElseThrow(
            () -> new RuntimeException("Chat no encontrado")
        );
        
        List<ChatParticipante> participantes = chatParticipanteRepositorio.findByChatChatId(chatId);
        
        Map<String, Object> info = new HashMap<>();
        info.put("chatId", chat.getChatId());
        info.put("nombreChat", chat.getNombreChat());
        info.put("tipo", chat.getTipo().toString());
        info.put("fechaCreacion", chat.getFechaCreacion());
        info.put("totalParticipantes", participantes.size());
        info.put("totalAdministradores", participantes.stream()
            .filter(p -> p.getEsAdmin() != null && p.getEsAdmin())
            .count());
        
        // Lista de administradores
        List<String> admins = participantes.stream()
            .filter(p -> p.getEsAdmin() != null && p.getEsAdmin())
            .map(p -> p.getUsuario().getUsername())
            .collect(Collectors.toList());
        info.put("administradores", admins);
        
        // Lista de todos los participantes
        List<String> todosParticipantes = participantes.stream()
            .map(p -> p.getUsuario().getUsername())
            .collect(Collectors.toList());
        info.put("participantes", todosParticipantes);
        
        return info;
    } 
    
    
    
    
    
    
}