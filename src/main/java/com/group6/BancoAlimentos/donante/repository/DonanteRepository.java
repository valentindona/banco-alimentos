package com.group6.BancoAlimentos.donante.repository;

import com.group6.BancoAlimentos.donante.modelo.Donante;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DonanteRepository extends JpaRepository<Donante, Long> {

    Optional<Donante> findByIdAndActivoTrue(Long id);
}
