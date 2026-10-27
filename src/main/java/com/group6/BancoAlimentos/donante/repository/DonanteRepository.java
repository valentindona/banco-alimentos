package com.group6.BancoAlimentos.donante.repository;

import com.group6.BancoAlimentos.donante.modelo.Donante;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DonanteRepository extends JpaRepository<Donante, Long> {

    Optional<Donante> findByIdAndActivoTrue(Long id);

    @Query("""
            select donante from Donante donante
            where (:nombre is null or lower(donante.nombre) like lower(concat('%', :nombre, '%')))
              and (:activo is null or donante.activo = :activo)
            """)
    Page<Donante> buscarConFiltros(
            @Param("nombre") String nombre, @Param("activo") Boolean activo, Pageable pageable);
}
