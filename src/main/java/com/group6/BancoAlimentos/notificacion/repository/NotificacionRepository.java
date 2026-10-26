package com.group6.BancoAlimentos.notificacion.repository;

import com.group6.BancoAlimentos.notificacion.modelo.Notificacion;
import com.group6.BancoAlimentos.notificacion.modelo.TipoNotificacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    boolean existsByStockIdAndTipo(Long stockId, TipoNotificacion tipo);

    Page<Notificacion> findAllByTipo(TipoNotificacion tipo, Pageable pageable);
}
