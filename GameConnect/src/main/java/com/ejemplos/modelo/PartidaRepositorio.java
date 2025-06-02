package com.ejemplos.modelo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PartidaRepositorio extends JpaRepository<Partida, Long> {
	
	List<Partida> findByUsuario(Usuario usuario);

}
