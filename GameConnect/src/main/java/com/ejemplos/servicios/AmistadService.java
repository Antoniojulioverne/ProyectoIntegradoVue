package com.ejemplos.servicios;

import com.ejemplos.modelo.*;
import com.ejemplos.DTO.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class AmistadService {
    
    @Autowired
    private AmistadRepositorio amistadRepositorio;
    
    @Autowired
    private UsuarioRepositorio usuarioRepositorio;
    @Autowired
    private PartidaRepositorio partidaRepositorio;
    @Autowired
    private PeticionAmistadRepositorio peticionAmistadRepositorio;
    
    @Autowired
    private ChatService chatService;
    
    @Autowired
    private PeticionAmistadDTOConverter peticionDTOConverter;
    
    public ChatDTO crearAmistad(Long usuarioId1, Long usuarioId2) {
        // Verificar si ya existe la amistad
        if (amistadRepositorio.existeAmistadEntreUsuarios(usuarioId1, usuarioId2)) {
            throw new RuntimeException("La amistad ya existe");
        }
        
        Usuario usuario1 = usuarioRepositorio.findById(usuarioId1).orElseThrow();
        Usuario usuario2 = usuarioRepositorio.findById(usuarioId2).orElseThrow();
        
        // Crear amistad (constructor ordena los IDs automáticamente)
        Amistad nuevaAmistad = new Amistad(usuario1, usuario2);
        amistadRepositorio.save(nuevaAmistad);
        
        // Crear chat automáticamente
        return chatService.crearChatPrivado(usuarioId1, usuarioId2);
    }
    
    public PeticionAmistadDTO enviarPeticionAmistad(Long solicitanteId, Long destinatarioId) {
        // Verificar que no sean el mismo usuario
        if (solicitanteId.equals(destinatarioId)) {
            throw new RuntimeException("No puedes enviarte una petición a ti mismo");
        }
        
        // Verificar que los usuarios existan y estén verificados
        Usuario solicitante = usuarioRepositorio.findById(solicitanteId)
            .orElseThrow(() -> new RuntimeException("Usuario solicitante no encontrado"));
        Usuario destinatario = usuarioRepositorio.findById(destinatarioId)
            .orElseThrow(() -> new RuntimeException("Usuario destinatario no encontrado"));
            
        if (!solicitante.isEmailVerificado() || !destinatario.isEmailVerificado()) {
            throw new RuntimeException("Ambos usuarios deben estar verificados");
        }
        
        // Verificar si ya son amigos
        if (amistadRepositorio.existeAmistadEntreUsuarios(solicitanteId, destinatarioId)) {
            throw new RuntimeException("Ya son amigos");
        }
        
        // Verificar si ya existe una petición pendiente
        if (peticionAmistadRepositorio.existePeticionPendienteEntreUsuarios(solicitanteId, destinatarioId)) {
            throw new RuntimeException("Ya existe una petición pendiente entre estos usuarios");
        }
        
        // Crear nueva petición
        PeticionAmistad peticion = PeticionAmistad.builder()
            .usuarioSolicitante(solicitante)
            .usuarioDestinatario(destinatario)
            .estado(PeticionAmistad.EstadoPeticion.PENDIENTE)
            .build();
            
        PeticionAmistad guardada = peticionAmistadRepositorio.save(peticion);
        return peticionDTOConverter.convertirADto(guardada);
    }
    
    public ChatDTO responderPeticionAmistad(Long peticionId, boolean aceptar) {
        PeticionAmistad peticion = peticionAmistadRepositorio.findById(peticionId)
            .orElseThrow(() -> new RuntimeException("Petición no encontrada"));
            
        if (peticion.getEstado() != PeticionAmistad.EstadoPeticion.PENDIENTE) {
            throw new RuntimeException("Esta petición ya fue respondida");
        }
        
        peticion.setFechaRespuesta(LocalDateTime.now());
        
        if (aceptar) {
            peticion.setEstado(PeticionAmistad.EstadoPeticion.ACEPTADA);
            peticionAmistadRepositorio.save(peticion);
            
            // Crear la amistad
            return crearAmistad(
                peticion.getUsuarioSolicitante().getUsuarioId(),
                peticion.getUsuarioDestinatario().getUsuarioId()
            );
        } else {
            peticion.setEstado(PeticionAmistad.EstadoPeticion.RECHAZADA);
            peticionAmistadRepositorio.save(peticion);
            return null;
        }
    }
    
    public List<PeticionAmistadDTO> obtenerPeticionesPendientes(Long usuarioId) {
        List<PeticionAmistad> peticiones = peticionAmistadRepositorio.findPeticionesPendientesRecibidas(usuarioId);
        return peticiones.stream()
            .map(peticionDTOConverter::convertirADto)
            .collect(Collectors.toList());
    }
    
    public List<PeticionAmistadDTO> obtenerPeticionesEnviadas(Long usuarioId) {
        List<PeticionAmistad> peticiones = peticionAmistadRepositorio.findPeticionesEnviadas(usuarioId);
        return peticiones.stream()
            .map(peticionDTOConverter::convertirADto)
            .collect(Collectors.toList());
    }
    
    public List<UsuarioBusquedaDTO> buscarUsuarios(String termino, Long usuarioActualId) {
        // Usar el nuevo método de repositorio que realiza la búsqueda en la base de datos
        List<Usuario> usuarios = usuarioRepositorio.buscarPorUsernameOEmail(termino, usuarioActualId);
            
        return usuarios.stream()
            .map(usuario -> {
                UsuarioBusquedaDTO dto = new UsuarioBusquedaDTO();
                dto.setUsuarioId(usuario.getUsuarioId());
                dto.setUsername(usuario.getUsername());
                dto.setEmail(usuario.getEmail());
                dto.setSkin(usuario.getSkin());
                dto.setFotoPerfil(usuario.getFotoPerfil()); // Asignar foto de perfil
                dto.setEmailVerificado(usuario.isEmailVerificado());
                
                // Determinar estado de relación
                String estadoRelacion = determinarEstadoRelacion(usuarioActualId, usuario.getUsuarioId());
                dto.setEstadoRelacion(estadoRelacion);
                
                // Si hay una petición pendiente recibida, incluir el ID de la petición
                if (estadoRelacion.equals("PETICION_RECIBIDA")) {
                    var peticion = peticionAmistadRepositorio.findPeticionEntreUsuarios(
                        usuario.getUsuarioId(), usuarioActualId);
                    peticion.ifPresent(p -> dto.setPeticionId(p.getPeticionId()));
                }
                
                // Log para depuración
                System.out.println("Usuario encontrado: " + usuario.getUsername() + 
                                  ", Foto: " + (usuario.getFotoPerfil() != null ? 
                                               (usuario.getFotoPerfil().length() > 20 ? 
                                               usuario.getFotoPerfil().substring(0, 20) + "..." : 
                                               usuario.getFotoPerfil()) : 
                                               "null"));
                
                return dto;
            })
            .collect(Collectors.toList());
    }
    
    private String determinarEstadoRelacion(Long usuarioActualId, Long otroUsuarioId) {
        // Verificar si ya son amigos
        if (amistadRepositorio.existeAmistadEntreUsuarios(usuarioActualId, otroUsuarioId)) {
            return "AMISTAD_EXISTENTE";
        }
        
        // Verificar peticiones pendientes
        var peticion = peticionAmistadRepositorio.findPeticionEntreUsuarios(usuarioActualId, otroUsuarioId);
        
        if (peticion.isPresent() && peticion.get().getEstado() == PeticionAmistad.EstadoPeticion.PENDIENTE) {
            if (peticion.get().getUsuarioSolicitante().getUsuarioId().equals(usuarioActualId)) {
                return "PETICION_ENVIADA";
            } else {
                return "PETICION_RECIBIDA";
            }
        }
        
        return "SIN_RELACION";
    }
    
   

  
}