package com.group6.BancoAlimentos.usuario.service;

import com.group6.BancoAlimentos.Security.TokenJwtService;
import com.group6.BancoAlimentos.usuario.dto.LoginSolicitud;
import com.group6.BancoAlimentos.usuario.dto.TokenRespuesta;
import com.group6.BancoAlimentos.usuario.modelo.Usuario;
import com.group6.BancoAlimentos.usuario.repository.UsuarioRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AutenticacionService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenJwtService tokenJwtService;

    public AutenticacionService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            TokenJwtService tokenJwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenJwtService = tokenJwtService;
    }

    public TokenRespuesta iniciarSesion(LoginSolicitud solicitud) {
        Usuario usuario = usuarioRepository.findByNombreUsuario(solicitud.nombreUsuario())
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas."));
        if (!usuario.isActivo()) {
            throw new DisabledException("La cuenta está deshabilitada.");
        }
        if (!passwordEncoder.matches(solicitud.clave(), usuario.getClaveHash())) {
            throw new BadCredentialsException("Credenciales inválidas.");
        }
        return tokenJwtService.crearRespuesta(usuario);
    }

    public String codificarClave(String clave) {
        return passwordEncoder.encode(clave);
    }
}
