package com.group6.BancoAlimentos.donacion.repository;

import com.group6.BancoAlimentos.donacion.modelo.Donacion;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DonacionRepository extends JpaRepository<Donacion, Long> {

    @Query("""
            select donacion from Donacion donacion
            where (:donanteId is null or donacion.donante.id = :donanteId)
              and (:fechaDesde is null or donacion.fecha >= :fechaDesde)
              and (:fechaHasta is null or donacion.fecha <= :fechaHasta)
              and (
                  (:productoId is null and :vencimientoDesde is null and :vencimientoHasta is null)
                  or exists (
                      select item.id from ItemDonacion item
                      where item.donacion = donacion
                        and (:productoId is null or item.producto.id = :productoId)
                        and (:vencimientoDesde is null or item.fechaVencimiento >= :vencimientoDesde)
                        and (:vencimientoHasta is null or item.fechaVencimiento <= :vencimientoHasta)
                  )
              )
            """)
    Page<Donacion> buscarConFiltros(
            @Param("donanteId") Long donanteId,
            @Param("fechaDesde") LocalDate fechaDesde,
            @Param("fechaHasta") LocalDate fechaHasta,
            @Param("productoId") Long productoId,
            @Param("vencimientoDesde") LocalDate vencimientoDesde,
            @Param("vencimientoHasta") LocalDate vencimientoHasta,
            Pageable pageable);
}
