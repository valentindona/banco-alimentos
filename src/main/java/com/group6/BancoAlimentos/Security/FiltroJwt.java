package com.group6.BancoAlimentos.Security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class FiltroJwt extends OncePerRequestFilter {

    private final TokenJwtService tokenJwtService;

    public FiltroJwt(TokenJwtService tokenJwtService) {
        this.tokenJwtService = tokenJwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest solicitud,
            HttpServletResponse respuesta,
            FilterChain cadena) throws ServletException, IOException {
        String encabezado = solicitud.getHeader("Authorization");
        if (encabezado != null && encabezado.startsWith("Bearer ")) {
            autenticar(encabezado.substring(7));
        }
        cadena.doFilter(solicitud, respuesta);
    }

    private void autenticar(String token) {
        try {
            IdentidadJwt identidad = tokenJwtService.validarToken(token);
            List<SimpleGrantedAuthority> autoridades = identidad.roles().stream()
                    .map(rol -> new SimpleGrantedAuthority("ROLE_" + rol))
                    .toList();
            UsuarioAutenticado principal = new UsuarioAutenticado(
                    identidad.usuarioId(),
                    identidad.nombreUsuario(),
                    identidad.roles(),
                    identidad.institucionId());
            var autenticacion = new UsernamePasswordAuthenticationToken(principal, null, autoridades);
            SecurityContextHolder.getContext().setAuthentication(autenticacion);
        } catch (JwtException excepcion) {
            SecurityContextHolder.clearContext();
        }
    }
}
