package com.group6.BancoAlimentos.factura.repository;

import com.group6.BancoAlimentos.factura.modelo.Factura;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface FacturaRepository extends JpaRepository<Factura, Long> {

    boolean existsByDonacionId(Long donacionId);

    @Query("""
            select factura from Factura factura
            where (:institucionId is null or factura.institucionId = :institucionId)
              and (:fechaDesde is null or factura.fecha >= :fechaDesde)
              and (:fechaHasta is null or factura.fecha <= :fechaHasta)
              and (:estado is null or lower(factura.estado) = lower(:estado))
            """)
    Page<Factura> buscarConFiltros(
            @Param("institucionId") Long institucionId,
            @Param("fechaDesde") LocalDate fechaDesde,
            @Param("fechaHasta") LocalDate fechaHasta,
            @Param("estado") String estado,
            Pageable pageable);
}
