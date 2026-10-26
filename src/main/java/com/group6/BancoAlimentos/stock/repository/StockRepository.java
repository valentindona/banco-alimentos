package com.group6.BancoAlimentos.stock.repository;

import com.group6.BancoAlimentos.stock.modelo.Stock;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByProductoIdAndFechaVencimiento(Long productoId, LocalDate fechaVencimiento);

    Optional<Stock> findByProductoIdAndFechaVencimientoIsNull(Long productoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select stock from Stock stock where stock.id in :ids order by stock.id")
    List<Stock> buscarTodosPorIdBloqueadosOrdenados(@Param("ids") Collection<Long> ids);
}
