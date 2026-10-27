package com.group6.BancoAlimentos.Common.handler;

import com.group6.BancoAlimentos.Common.excepcion.ReglaNegocioException;
import com.group6.BancoAlimentos.Common.excepcion.RecursoNoEncontradoException;
import com.group6.BancoAlimentos.Common.excepcion.SolicitudInvalidaException;
import com.group6.BancoAlimentos.Common.respuesta.ErrorRespuesta;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ReglaNegocioException.class)
    ResponseEntity<ErrorRespuesta> manejarReglaNegocio(
            ReglaNegocioException excepcion,
            HttpServletRequest solicitud) {
        return crearRespuesta(excepcion.getEstadoHttp(), excepcion.getCodigo(),
                excepcion.getMessage(), solicitud, List.of());
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    ResponseEntity<ErrorRespuesta> manejarRecursoNoEncontrado(
            RecursoNoEncontradoException excepcion,
            HttpServletRequest solicitud) {
        return crearRespuesta(HttpStatus.NOT_FOUND, "RECURSO_INEXISTENTE",
                excepcion.getMessage(), solicitud, List.of());
    }

    @ExceptionHandler(SolicitudInvalidaException.class)
    ResponseEntity<ErrorRespuesta> manejarSolicitudInvalida(
            SolicitudInvalidaException excepcion,
            HttpServletRequest solicitud) {
        return crearRespuesta(HttpStatus.BAD_REQUEST, "SOLICITUD_INVALIDA",
                excepcion.getMessage(), solicitud, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorRespuesta> manejarValidacion(
            MethodArgumentNotValidException excepcion,
            HttpServletRequest solicitud) {
        List<ErrorRespuesta.ErrorCampo> errores = excepcion.getBindingResult().getFieldErrors().stream()
                .map(this::convertirErrorCampo)
                .toList();
        return crearRespuesta(HttpStatus.BAD_REQUEST, "SOLICITUD_INVALIDA",
                "La solicitud contiene campos inválidos.", solicitud, errores);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ErrorRespuesta> manejarRestricciones(
            ConstraintViolationException excepcion,
            HttpServletRequest solicitud) {
        List<ErrorRespuesta.ErrorCampo> errores = excepcion.getConstraintViolations().stream()
                .map(error -> new ErrorRespuesta.ErrorCampo(
                        error.getPropertyPath().toString(), error.getMessage()))
                .toList();
        return crearRespuesta(HttpStatus.BAD_REQUEST, "SOLICITUD_INVALIDA",
                "La solicitud contiene campos inválidos.", solicitud, errores);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorRespuesta> manejarCuerpoInvalido(
            HttpMessageNotReadableException excepcion,
            HttpServletRequest solicitud) {
        return crearRespuesta(HttpStatus.BAD_REQUEST, "SOLICITUD_INVALIDA",
                "El cuerpo de la solicitud no tiene un formato válido.", solicitud, List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErrorRespuesta> manejarParametroInvalido(
            MethodArgumentTypeMismatchException excepcion,
            HttpServletRequest solicitud) {
        List<ErrorRespuesta.ErrorCampo> errores = List.of(
                new ErrorRespuesta.ErrorCampo(excepcion.getName(), "El formato del parámetro no es válido."));
        return crearRespuesta(HttpStatus.BAD_REQUEST, "SOLICITUD_INVALIDA",
                "La solicitud contiene parámetros inválidos.", solicitud, errores);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ErrorRespuesta> manejarAutenticacion(
            AuthenticationException excepcion,
            HttpServletRequest solicitud) {
        return crearRespuesta(HttpStatus.UNAUTHORIZED, "NO_AUTENTICADO",
                "Se requiere autenticación válida.", solicitud, List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorRespuesta> manejarAccesoDenegado(
            AccessDeniedException excepcion,
            HttpServletRequest solicitud) {
        return crearRespuesta(HttpStatus.FORBIDDEN, "ACCESO_DENEGADO",
                "No tiene permiso para realizar esta operación.", solicitud, List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorRespuesta> manejarRecursoInexistente(
            NoResourceFoundException excepcion,
            HttpServletRequest solicitud) {
        return crearRespuesta(HttpStatus.NOT_FOUND, "RECURSO_INEXISTENTE",
                "El recurso solicitado no existe.", solicitud, List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErrorRespuesta> manejarMetodoNoPermitido(
            HttpRequestMethodNotSupportedException excepcion,
            HttpServletRequest solicitud) {
        return crearRespuesta(HttpStatus.METHOD_NOT_ALLOWED, "METODO_NO_PERMITIDO",
                "El método solicitado no está permitido para este recurso.", solicitud, List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorRespuesta> manejarErrorInesperado(
            Exception excepcion,
            HttpServletRequest solicitud) {
        LOGGER.error("Error inesperado en {}", solicitud.getRequestURI(), excepcion);
        return crearRespuesta(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO",
                "Ocurrió un error inesperado.", solicitud, List.of());
    }

    private ErrorRespuesta.ErrorCampo convertirErrorCampo(FieldError error) {
        return new ErrorRespuesta.ErrorCampo(error.getField(), error.getDefaultMessage());
    }

    private ResponseEntity<ErrorRespuesta> crearRespuesta(
            HttpStatus estado,
            String codigo,
            String mensaje,
            HttpServletRequest solicitud,
            List<ErrorRespuesta.ErrorCampo> errores) {
        ErrorRespuesta respuesta = new ErrorRespuesta(
                OffsetDateTime.now(ZoneOffset.UTC),
                estado.value(),
                codigo,
                mensaje,
                solicitud.getRequestURI(),
                errores);
        return ResponseEntity.status(estado).body(respuesta);
    }
}
