package com.group6.BancoAlimentos.Common.respuesta;

import java.util.List;
import org.springframework.data.domain.Page;

public record PaginaRespuesta<T>(
        List<T> contenido,
        int pagina,
        int tamano,
        long totalElementos,
        int totalPaginas) {

    public static <T> PaginaRespuesta<T> desde(Page<T> resultado) {
        return new PaginaRespuesta<>(
                resultado.getContent(),
                resultado.getNumber(),
                resultado.getSize(),
                resultado.getTotalElements(),
                resultado.getTotalPages());
    }
}
