package com.group6.BancoAlimentos.stock.repository;

import com.group6.BancoAlimentos.stock.modelo.Stock;
import com.group6.BancoAlimentos.stock.modelo.EstadoStock;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

public interface StockRepository extends JpaRepository<Stock, Long> {

    @Query("""
            select stock from Stock stock
            where (:productoId is null or stock.productoId = :productoId)
              and (:fechaVencimiento is null or stock.fechaVencimiento = :fechaVencimiento)
              and (:estado is null or stock.estado = :estado)
            """)
    Page<Stock> buscarConFiltros(
            @Param("productoId") Long productoId,
            @Param("fechaVencimiento") LocalDate fechaVencimiento,
            @Param("estado") EstadoStock estado,
            Pageable pageable);

    Optional<Stock> findByProductoIdAndFechaVencimiento(Long productoId, LocalDate fechaVencimiento);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select stock from Stock stock
            where stock.productoId = :productoId and stock.fechaVencimiento = :fechaVencimiento
            """)
    Optional<Stock> buscarPorProductoYVencimientoBloqueado(
            @Param("productoId") Long productoId, @Param("fechaVencimiento") LocalDate fechaVencimiento);

    Optional<Stock> findByProductoIdAndFechaVencimientoIsNull(Long productoId);

    List<Stock> findByFechaVencimientoLessThanEqualAndCantidadDisponibleGreaterThanOrderByIdAsc(
            LocalDate fechaVencimiento, BigDecimal cantidadDisponible);

    List<Stock> findByFechaVencimientoBetweenAndCantidadDisponibleGreaterThanOrderByIdAsc(
            LocalDate fechaDesde, LocalDate fechaHasta, BigDecimal cantidadDisponible);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select stock from Stock stock where stock.id in :ids order by stock.id")
    List<Stock> buscarTodosPorIdBloqueadosOrdenados(@Param("ids") Collection<Long> ids);
}
