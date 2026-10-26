package com.group6.BancoAlimentos.remito.repository;

import com.group6.BancoAlimentos.remito.modelo.Remito;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RemitoRepository extends JpaRepository<Remito, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select remito from Remito remito where remito.id = :id")
    Optional<Remito> buscarPorIdBloqueado(@Param("id") Long id);
}
