package com.group6.BancoAlimentos.ajuste.repository;

import com.group6.BancoAlimentos.ajuste.modelo.Ajuste;
import com.group6.BancoAlimentos.ajuste.modelo.TipoAjuste;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.Repository;

public interface AjusteRepository extends Repository<Ajuste, Long> {

    Ajuste save(Ajuste ajuste);

    Optional<Ajuste> findById(Long id);

    void delete(Ajuste ajuste);

    @Query(value = """
            SELECT COUNT(*)
            FROM stock_aud stockAudit
            WHERE stockAudit.id = :stockId
              AND stockAudit.REV > (
                  SELECT COALESCE(MAX(ajusteAudit.REV), 0)
                  FROM ajustes_stock_aud ajusteAudit
                  WHERE ajusteAudit.id = :ajusteId
              )
            """, nativeQuery = true)
    int contarMovimientosStockPosteriores(
            @Param("ajusteId") Long ajusteId, @Param("stockId") Long stockId);

    @Query("""
            select ajuste from Ajuste ajuste
            where (:productoId is null or ajuste.productoId = :productoId)
              and (:fechaVencimiento is null or ajuste.fechaVencimiento = :fechaVencimiento)
              and (:fechaDesde is null or ajuste.fechaHora >= :fechaDesde)
              and (:fechaHastaExclusiva is null or ajuste.fechaHora < :fechaHastaExclusiva)
              and (:usuarioId is null or ajuste.usuarioId = :usuarioId)
              and (:tipo is null or ajuste.tipo = :tipo)
            """)
    Page<Ajuste> buscarConFiltros(
            @Param("productoId") Long productoId,
            @Param("fechaVencimiento") java.time.LocalDate fechaVencimiento,
            @Param("fechaDesde") Instant fechaDesde,
            @Param("fechaHastaExclusiva") Instant fechaHastaExclusiva,
            @Param("usuarioId") Long usuarioId,
            @Param("tipo") TipoAjuste tipo,
            Pageable pageable);
}
