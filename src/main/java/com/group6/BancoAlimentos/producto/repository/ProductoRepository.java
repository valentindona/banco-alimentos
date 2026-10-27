package com.group6.BancoAlimentos.producto.repository;

import com.group6.BancoAlimentos.producto.modelo.Producto;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    Optional<Producto> findByIdAndActivoTrue(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select producto from Producto producto where producto.id = :id and producto.activo = true")
    Optional<Producto> buscarActivoBloqueado(@Param("id") Long id);

    @Query("""
            select producto from Producto producto
            where (:nombre is null or lower(producto.nombre) like lower(concat('%', :nombre, '%')))
              and (:tipoAlimento is null or lower(producto.tipoAlimento) = lower(:tipoAlimento))
              and (:unidadMedida is null or lower(producto.unidadMedida) = lower(:unidadMedida))
              and (:activo is null or producto.activo = :activo)
            """)
    Page<Producto> buscarConFiltros(
            @Param("nombre") String nombre,
            @Param("tipoAlimento") String tipoAlimento,
            @Param("unidadMedida") String unidadMedida,
            @Param("activo") Boolean activo,
            Pageable pageable);
}
