package com.group6.BancoAlimentos.Security;

import com.group6.BancoAlimentos.Common.respuesta.ErrorRespuesta;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class ManejadorErroresSeguridad {

    private final ObjectMapper objectMapper;

    public ManejadorErroresSeguridad(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void escribir(HttpServletRequest solicitud, HttpServletResponse respuesta, HttpStatus estado)
            throws IOException {
        String codigo = estado == HttpStatus.UNAUTHORIZED ? "NO_AUTENTICADO" : "ACCESO_DENEGADO";
        String mensaje = estado == HttpStatus.UNAUTHORIZED
                ? "Se requiere autenticación válida."
                : "No tiene permiso para realizar esta operación.";
        ErrorRespuesta error = new ErrorRespuesta(
                OffsetDateTime.now(ZoneOffset.UTC),
                estado.value(),
                codigo,
                mensaje,
                solicitud.getRequestURI(),
                List.of());
        respuesta.setStatus(estado.value());
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(respuesta.getOutputStream(), error);
    }
}
