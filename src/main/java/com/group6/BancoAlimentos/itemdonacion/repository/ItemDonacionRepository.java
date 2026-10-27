package com.group6.BancoAlimentos.itemdonacion.repository;

import com.group6.BancoAlimentos.itemdonacion.modelo.ItemDonacion;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemDonacionRepository extends JpaRepository<ItemDonacion, Long> {

    List<ItemDonacion> findByDonacionIdOrderByIdAsc(Long donacionId);

    @Query("""
            select item from ItemDonacion item
            where item.donacion.id in :donacionIds
            order by item.donacion.id, item.id
            """)
    List<ItemDonacion> buscarPorDonaciones(@Param("donacionIds") Collection<Long> donacionIds);
}
