package com.group6.BancoAlimentos.institucion.repository;

import com.group6.BancoAlimentos.institucion.modelo.Institucion;
import com.group6.BancoAlimentos.institucion.modelo.EstadoPago;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InstitucionRepository extends JpaRepository<Institucion, Long> {

    @Query("""
            select institucion from Institucion institucion
            where (:nombre is null or lower(institucion.nombre) like lower(concat('%', :nombre, '%')))
              and (:estadoPago is null or institucion.estadoPago = :estadoPago)
            """)
    Page<Institucion> buscarConFiltros(
            @Param("nombre") String nombre,
            @Param("estadoPago") EstadoPago estadoPago,
            Pageable pageable);
}
