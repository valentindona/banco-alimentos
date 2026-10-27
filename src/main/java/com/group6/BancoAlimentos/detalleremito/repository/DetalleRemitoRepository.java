package com.group6.BancoAlimentos.detalleremito.repository;

import com.group6.BancoAlimentos.detalleremito.modelo.DetalleRemito;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetalleRemitoRepository extends JpaRepository<DetalleRemito, Long> {

    boolean existsByRemitoIdAndStockId(Long remitoId, Long stockId);

    List<DetalleRemito> findByRemitoIdOrderByStockIdAsc(Long remitoId);

    Optional<DetalleRemito> findByIdAndRemitoId(Long id, Long remitoId);
}
