package com.group6.BancoAlimentos.itemdonacion.repository;

import com.group6.BancoAlimentos.itemdonacion.modelo.ItemDonacion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemDonacionRepository extends JpaRepository<ItemDonacion, Long> {

    List<ItemDonacion> findByDonacionIdOrderByIdAsc(Long donacionId);
}
