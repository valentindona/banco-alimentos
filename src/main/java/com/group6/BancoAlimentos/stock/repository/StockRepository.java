package com.group6.BancoAlimentos.stock.repository;

import com.group6.BancoAlimentos.stock.modelo.Stock;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByProductoIdAndFechaVencimiento(Long productoId, LocalDate fechaVencimiento);

    Optional<Stock> findByProductoIdAndFechaVencimientoIsNull(Long productoId);
}
