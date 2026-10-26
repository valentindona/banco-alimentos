package com.group6.BancoAlimentos.producto.repository;

import com.group6.BancoAlimentos.producto.modelo.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    @Query("""
            select producto from Producto producto
            where (:nombre is null or lower(producto.nombre) like lower(concat('%', :nombre, '%')))
              and (:activo is null or producto.activo = :activo)
            """)
    Page<Producto> buscarConFiltros(
            @Param("nombre") String nombre,
            @Param("activo") Boolean activo,
            Pageable pageable);
}
