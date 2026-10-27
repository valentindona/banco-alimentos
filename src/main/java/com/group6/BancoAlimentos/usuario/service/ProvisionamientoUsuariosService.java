package com.group6.BancoAlimentos.usuario.service;

import com.group6.BancoAlimentos.Common.excepcion.ReglaNegocioException;
import com.group6.BancoAlimentos.institucion.service.ConsultaInstitucionService;
import com.group6.BancoAlimentos.usuario.dto.AltaUsuarioSolicitud;
import com.group6.BancoAlimentos.usuario.dto.UsuarioRespuesta;
import com.group6.BancoAlimentos.usuario.modelo.RolUsuario;
import com.group6.BancoAlimentos.usuario.modelo.Usuario;
import com.group6.BancoAlimentos.usuario.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProvisionamientoUsuariosService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final ConsultaInstitucionService consultaInstitucionService;

    public ProvisionamientoUsuariosService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            ConsultaInstitucionService consultaInstitucionService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.consultaInstitucionService = consultaInstitucionService;
    }

    @Transactional
    public UsuarioRespuesta crear(AltaUsuarioSolicitud solicitud) {
        if (usuarioRepository.existsByNombreUsuario(solicitud.nombreUsuario())) {
            throw new ReglaNegocioException("USUARIO_DUPLICADO", "El nombre de usuario ya está registrado.");
        }
        if (solicitud.institucionId() != null
                && !consultaInstitucionService.existeActiva(solicitud.institucionId())) {
            throw new ReglaNegocioException("INSTITUCION_INEXISTENTE",
                    "La institución indicada no existe o no está activa.");
        }
        Usuario usuario = Usuario.builder()
                .nombreUsuario(solicitud.nombreUsuario())
                .claveHash(passwordEncoder.encode(solicitud.clave()))
                .rol(solicitud.rol())
                .institucionId(solicitud.institucionId())
                .activo(true)
                .build();
        return convertirRespuesta(usuarioRepository.save(usuario));
    }

    @Transactional
    public void crearUsuarioInicial(String nombreUsuario, String clave, RolUsuario rol) {
        crearUsuarioInicial(nombreUsuario, clave, rol, null);
    }

    @Transactional
    public void crearUsuarioInicial(String nombreUsuario, String clave, RolUsuario rol, Long institucionId) {
        if (usuarioRepository.existsByNombreUsuario(nombreUsuario)) {
            return;
        }
        if (rol == RolUsuario.USER_INSTITUCIONAL) {
            if (institucionId == null) {
                throw new ReglaNegocioException(
                        "INSTITUCION_REQUERIDA", "El usuario institucional debe estar asociado a una institución.");
            }
            if (!consultaInstitucionService.existeActiva(institucionId)) {
                throw new ReglaNegocioException("INSTITUCION_INEXISTENTE",
                        "La institución indicada no existe o no está activa.");
            }
        } else if (institucionId != null) {
            throw new ReglaNegocioException(
                    "INSTITUCION_NO_PERMITIDA", "Solo los usuarios institucionales pueden asociarse a una institución.");
        }
        Usuario usuario = Usuario.builder()
                .nombreUsuario(nombreUsuario)
                .claveHash(passwordEncoder.encode(clave))
                .rol(rol)
                .institucionId(institucionId)
                .activo(true)
                .build();
        usuarioRepository.save(usuario);
    }

    private UsuarioRespuesta convertirRespuesta(Usuario usuario) {
        return new UsuarioRespuesta(
                usuario.getId(),
                usuario.getNombreUsuario(),
                usuario.getRol(),
                usuario.getInstitucionId(),
                usuario.isActivo());
    }
}
