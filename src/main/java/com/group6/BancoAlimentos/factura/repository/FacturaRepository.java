package com.group6.BancoAlimentos.factura.repository;

import com.group6.BancoAlimentos.factura.modelo.Factura;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface FacturaRepository extends JpaRepository<Factura, Long> {

    boolean existsByDonacionId(Long donacionId);

    Optional<Factura> findByDonacionId(Long donacionId);

    List<Factura> findByDonacionIdIn(Collection<Long> donacionIds);

    @Query("""
            select factura from Factura factura
            where (:institucionId is null or factura.institucionId = :institucionId)
              and (:donanteId is null or factura.donanteId = :donanteId)
              and (:fechaDesde is null or factura.fecha >= :fechaDesde)
              and (:fechaHasta is null or factura.fecha <= :fechaHasta)
              and (:estado is null or lower(factura.estado) = lower(:estado))
              and (
                  (:productoId is null and :fechaVencimiento is null)
                  or exists (
                      select item.id from ItemDonacion item
                      where item.donacion.id = factura.donacionId
                        and (:productoId is null or item.producto.id = :productoId)
                        and (:fechaVencimiento is null or item.fechaVencimiento = :fechaVencimiento)
                  )
              )
            """)
    Page<Factura> buscarConFiltros(
            @Param("institucionId") Long institucionId,
            @Param("donanteId") Long donanteId,
            @Param("productoId") Long productoId,
            @Param("fechaDesde") LocalDate fechaDesde,
            @Param("fechaHasta") LocalDate fechaHasta,
            @Param("fechaVencimiento") LocalDate fechaVencimiento,
            @Param("estado") String estado,
            Pageable pageable);
}
