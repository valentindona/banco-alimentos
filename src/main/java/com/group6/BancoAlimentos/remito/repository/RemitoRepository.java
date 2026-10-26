package com.group6.BancoAlimentos.remito.repository;

import com.group6.BancoAlimentos.remito.modelo.Remito;
import com.group6.BancoAlimentos.remito.modelo.EstadoRemito;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RemitoRepository extends JpaRepository<Remito, Long> {

    @Query("""
            select remito from Remito remito
            where (:institucionId is null or remito.institucionId = :institucionId)
              and (:fechaDesde is null or remito.fechaEntrega >= :fechaDesde)
              and (:fechaHasta is null or remito.fechaEntrega <= :fechaHasta)
              and (:estado is null or remito.estado = :estado)
            """)
    Page<Remito> buscarConFiltros(
            @Param("institucionId") Long institucionId,
            @Param("fechaDesde") LocalDate fechaDesde,
            @Param("fechaHasta") LocalDate fechaHasta,
            @Param("estado") EstadoRemito estado,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select remito from Remito remito where remito.id = :id")
    Optional<Remito> buscarPorIdBloqueado(@Param("id") Long id);
}
