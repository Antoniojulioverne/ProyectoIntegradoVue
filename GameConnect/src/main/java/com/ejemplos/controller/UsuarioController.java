package com.ejemplos.controller;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ejemplos.DTO.*;
import com.ejemplos.modelo.*;
import com.ejemplos.servicios.AmistadService;
import com.ejemplos.servicios.ChatService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/GameConnect")
public class UsuarioController {
	
	
	@Autowired
	private final ChatService chatService;
	
	@Autowired
	private final UsuarioRepositorio usuarioRepositorio;

	@Autowired
	private final AmistadRepositorio amistadRepositorio;
	
	@Autowired
	private final PartidaRepositorio partidaRepositorio;
	
	@Autowired
	private final UsuarioDTOConverter usuarioDTOConverter;
	
	@Autowired
	private final PartidaDTOConverter partidaDTOConverter;
	
	
	@Autowired
	private final AmistadService amistadService;

	
	SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
	Date nuevaDate=null;
	
	@PostMapping("/usuario")
	public ResponseEntity<?> nuevoSendero(@RequestBody CreateUsuarioDTO nuevo) {	
		Usuario n = usuarioDTOConverter.convertirAUsuario(nuevo);
		
		return ResponseEntity.status(HttpStatus.CREATED).body(usuarioRepositorio.save(n));
	}
	@GetMapping("/usuario")
	public ResponseEntity<?> obtenerTodos(){
		List<Usuario> result = usuarioRepositorio.findAll();
		
		if(result.isEmpty()) {
			return ResponseEntity.notFound().build();
			
		}else {
			List<UsuarioDTO> dtoList = result.stream().map(usuarioDTOConverter::convertirADto).collect(Collectors.toList());
			return ResponseEntity.ok(dtoList);
		}
	}

	@GetMapping("/usuario/{id}")
	public ResponseEntity<?> obetenerUno(@PathVariable Long id) {
		
		Usuario result =usuarioRepositorio.findById(id).orElse(null);
		if (result ==null) {
			return ResponseEntity.notFound().build();
		}else {
			return ResponseEntity.ok(result);
		}
	}
	
	@PutMapping("/usuario/{id}")
    public ResponseEntity<?> editarsendero(@RequestBody CreateUsuarioDTO editar, @PathVariable Long id){
        if(usuarioRepositorio.existsById(id)) {
            Usuario n = usuarioDTOConverter.convertirAUsuario(editar);
            n.setUsuarioId(id);
            if(editar.getUsername()==null)
                n.setUsername(usuarioRepositorio.findById(id).get().getUsername());
            if(editar.getSkin()==null)
                n.setSkin(usuarioRepositorio.findById(id).get().getSkin());
            if(editar.getEmail()==null)
                n.setEmail(usuarioRepositorio.findById(id).get().getEmail());
            if(editar.getPassword()==null)
                n.setPassword(usuarioRepositorio.findById(id).get().getPassword());
            return ResponseEntity.ok(usuarioRepositorio.save(n));
        }else {
            return ResponseEntity.notFound().build();
        }
    }

	@DeleteMapping("/usuario/{id}")
	public ResponseEntity<Object> borrarSendero(@PathVariable Long id) {
		if (usuarioRepositorio.existsById(id)) {
			Usuario result = usuarioRepositorio.findById(id).get();
			usuarioRepositorio.deleteById(id);
			return ResponseEntity.noContent().build();
		}else {
			return ResponseEntity.notFound().build();
		}
	}
	
	@GetMapping("/usuario/{id}/amigos")
	public ResponseEntity<?> obtenerAmigosDeUsuario(@PathVariable Long id) {
	    Usuario usuario = usuarioRepositorio.findById(id).orElse(null);
	    if (usuario == null) {
	        return ResponseEntity.notFound().build();
	    }

	    // 👈 CAMBIAR ESTO - usar el nuevo repositorio
	    List<Amistad> amistades = amistadRepositorio.findAmistadesByUsuarioId(id);
	    
	    List<UsuarioDTO> amigosDTO = amistades.stream()
	            .map(amistad -> {
	                // Obtener el otro usuario de la amistad
	                Usuario amigo = amistad.getUsuario().getUsuarioId().equals(id) 
	                    ? amistad.getAmigo() 
	                    : amistad.getUsuario();
	                return usuarioDTOConverter.convertirADto(amigo);
	            })
	            .collect(Collectors.toList());

	    return ResponseEntity.ok(amigosDTO);
	}

	@PostMapping("/usuario/amigo")
	public ResponseEntity<?> agregarAmigoYCrearChat(@RequestBody AgregarAmigoDTO dto) {
	    try {
	        // 👈 CAMBIAR ESTO - usar el servicio
	        ChatDTO chat = amistadService.crearAmistad(dto.getUsuario1Id(), dto.getUsuario2Id());
	        return ResponseEntity.status(HttpStatus.CREATED).body(chat);
	    } catch (RuntimeException e) {
	        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
	    }
	}
	
	
	
	@GetMapping("/usuario/{id}/partidas")
	public ResponseEntity<?> obtenerPartidasDeUsuario(@PathVariable Long id) {
	    Usuario usuario = usuarioRepositorio.findById(id).orElse(null);
	    if (usuario == null) {
	        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado");
	    }

	    List<Partida> partidas = partidaRepositorio.findByUsuario(usuario);

	    if (partidas.isEmpty()) {
	        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("El usuario no tiene partidas registradas");
	    }

	    List<PartidaDTO> partidasDTO = partidas.stream()
	            .map(partidaDTOConverter::convertirADto)
	            .collect(Collectors.toList());

	    return ResponseEntity.ok(partidasDTO);
	}

	@PutMapping("/usuario/{id}/skin")
	public ResponseEntity<?> actualizarSkin(@PathVariable Long id, @RequestBody SkinRequestDTO skinRequest) {
	    Usuario usuario = usuarioRepositorio.findById(id).orElse(null);
	    if (usuario == null) {
	        return ResponseEntity.notFound().build();
	    }

	    usuario.setSkin(skinRequest.getSkin()); // 👈 Ahora extraes correctamente el valor
	    usuarioRepositorio.save(usuario);
	    return ResponseEntity.ok("Skin actualizada correctamente");
	}
	
	
	@GetMapping("/usuario/{id}/puntos")
	public ResponseEntity<?> obtenerPuntosTotales(@PathVariable Long id) {
	    Usuario usuario = usuarioRepositorio.findById(id).orElse(null);
	    if (usuario == null) {
	        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado");
	    }

	    List<Partida> partidas = partidaRepositorio.findByUsuario(usuario);
	    int puntosTotales = partidas.stream().mapToInt(Partida::getPuntos).sum();

	    return ResponseEntity.ok(puntosTotales);
	}

	@GetMapping("/usuario/{id}/monedas")
	public ResponseEntity<?> obtenerMonedasTotales(@PathVariable Long id) {
	    Usuario usuario = usuarioRepositorio.findById(id).orElse(null);
	    if (usuario == null) {
	        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado");
	    }

	    List<Partida> partidas = partidaRepositorio.findByUsuario(usuario);
	    int monedasTotales = partidas.stream().mapToInt(Partida::getMonedas).sum();

	    return ResponseEntity.ok(monedasTotales);
	}
	
	
	@GetMapping("/usuario/ranking")
	public ResponseEntity<?> obtenerRankingUsuarios() {
		List<Usuario> usuarios = usuarioRepositorio.findAll();
		
		List<UsuarioRankingDTO> ranking = usuarios.stream()
				.map(usuario -> {
					List<Partida> partidas = partidaRepositorio.findByUsuario(usuario);
					
					return partidas.stream()
							.max((p1, p2) -> Integer.compare(p1.getPuntos(), p2.getPuntos()))
							.map(partidaMaxima -> new UsuarioRankingDTO(
									usuario.getUsername(),
									partidaMaxima.getPuntos(),
									partidaMaxima.getFecha()
									))
							.orElse(new UsuarioRankingDTO( // ← aquí el cambio importante
									usuario.getUsername(),
									0,
									null
									));
				})
				.sorted((r1, r2) -> Integer.compare(r2.getPuntosMaximos(), r1.getPuntosMaximos())) // orden descendente
				.collect(Collectors.toList());
		
		return ResponseEntity.ok(ranking);
	}
	@PostMapping("/partida")
	public ResponseEntity<?> crearPartida(@RequestBody CreatePartidaDTO nuevaPartida) {
	    Usuario usuario = usuarioRepositorio.findById(nuevaPartida.getUsuarioId()).orElse(null);
	    if (usuario == null) {
	        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado");
	    }

	    Partida partida = new Partida();
	    partida.setUsuario(usuario);
	    partida.setPuntos(nuevaPartida.getPuntos());
	    partida.setMonedas(nuevaPartida.getMonedas());
	    partida.setFecha(LocalDateTime.now());

	    Partida guardada = partidaRepositorio.save(partida);
	    return ResponseEntity.status(HttpStatus.CREATED).body(partidaDTOConverter.convertirADto(guardada));
	}
	
	@GetMapping("/usuario/buscar")
	public ResponseEntity<?> buscarUsuarios(@RequestParam String termino, @RequestParam Long usuarioActualId) {
	    if (termino == null || termino.trim().isEmpty()) {
	        return ResponseEntity.badRequest().body("El término de búsqueda no puede estar vacío");
	    }
	    
	    try {
	        List<UsuarioBusquedaDTO> usuarios = amistadService.buscarUsuarios(termino, usuarioActualId);
	        return ResponseEntity.ok(usuarios);
	    } catch (Exception e) {
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
	    }
	}

	// Enviar petición de amistad
	@PostMapping("/usuario/{id}/peticion-amistad")
	public ResponseEntity<?> enviarPeticionAmistad(@PathVariable Long id, @RequestBody EnviarPeticionDTO dto) {
	    try {
	        PeticionAmistadDTO peticion = amistadService.enviarPeticionAmistad(id, dto.getUsuarioDestinatarioId());
	        return ResponseEntity.status(HttpStatus.CREATED).body(peticion);
	    } catch (RuntimeException e) {
	        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
	    }
	}

	// Obtener peticiones recibidas (pendientes)
	@GetMapping("/usuario/{id}/peticiones-recibidas")
	public ResponseEntity<?> obtenerPeticionesPendientes(@PathVariable Long id) {
	    try {
	        List<PeticionAmistadDTO> peticiones = amistadService.obtenerPeticionesPendientes(id);
	        return ResponseEntity.ok(peticiones);
	    } catch (Exception e) {
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
	    }
	}

	// Obtener peticiones enviadas
	@GetMapping("/usuario/{id}/peticiones-enviadas")
	public ResponseEntity<?> obtenerPeticionesEnviadas(@PathVariable Long id) {
	    try {
	        List<PeticionAmistadDTO> peticiones = amistadService.obtenerPeticionesEnviadas(id);
	        return ResponseEntity.ok(peticiones);
	    } catch (Exception e) {
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
	    }
	}

	// Responder petición de amistad
	@PutMapping("/peticion-amistad/{peticionId}/responder")
	public ResponseEntity<?> responderPeticionAmistad(@PathVariable Long peticionId, @RequestBody ResponderPeticionDTO dto) {
	    try {
	        ChatDTO chat = amistadService.responderPeticionAmistad(peticionId, dto.isAceptar());
	        
	        if (chat != null) {
	            return ResponseEntity.ok(Map.of(
	                "mensaje", "Petición aceptada. Se ha creado un chat automáticamente.",
	                "chat", chat
	            ));
	        } else {
	            return ResponseEntity.ok(Map.of("mensaje", "Petición rechazada"));
	        }
	    } catch (RuntimeException e) {
	        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
	    }
	}
}
