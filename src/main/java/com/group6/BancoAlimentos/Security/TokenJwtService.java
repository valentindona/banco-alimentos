package com.group6.BancoAlimentos.Security;

import com.group6.BancoAlimentos.Config.PropiedadesJwt;
import com.group6.BancoAlimentos.usuario.dto.TokenRespuesta;
import com.group6.BancoAlimentos.usuario.modelo.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class TokenJwtService {

    private static final String ALGORITMO_FIRMA = Jwts.SIG.HS256.getId();
    private static final String CLAIM_NOMBRE_USUARIO = "nombreUsuario";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_INSTITUCION_ID = "institucionId";

    private final PropiedadesJwt propiedadesJwt;
    private final Clock reloj;

    public TokenJwtService(PropiedadesJwt propiedadesJwt, Clock reloj) {
        this.propiedadesJwt = propiedadesJwt;
        this.reloj = reloj;
    }

    public TokenRespuesta crearRespuesta(Usuario usuario) {
        return new TokenRespuesta(generarToken(usuario), "Bearer", propiedadesJwt.getDuracion().toSeconds());
    }

    public String generarToken(Usuario usuario) {
        Instant ahora = reloj.instant();
        var creador = Jwts.builder()
                .subject(usuario.getId().toString())
                .claim(CLAIM_NOMBRE_USUARIO, usuario.getNombreUsuario())
                .claim(CLAIM_ROLES, List.of(usuario.getRol().nombre()))
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(propiedadesJwt.getDuracion())));
        if (usuario.getInstitucionId() != null) {
            creador.claim(CLAIM_INSTITUCION_ID, usuario.getInstitucionId());
        }
        return creador.signWith(claveFirma(), Jwts.SIG.HS256).compact();
    }

    public IdentidadJwt validarToken(String token) {
        var firmado = Jwts.parser()
                .verifyWith(claveFirma())
                .clock(() -> Date.from(reloj.instant()))
                .build()
                .parseSignedClaims(token);

        if (!ALGORITMO_FIRMA.equals(firmado.getHeader().get(Header.ALGORITHM))) {
            throw new JwtException("El algoritmo de firma no está permitido.");
        }
        Claims claims = firmado.getPayload();
        long usuarioId = convertirUsuarioId(claims.getSubject());
        String nombreUsuario = claims.get(CLAIM_NOMBRE_USUARIO, String.class);
        List<String> roles = convertirRoles(claims.get(CLAIM_ROLES));
        Long institucionId = claims.get(CLAIM_INSTITUCION_ID, Long.class);
        if (nombreUsuario == null || nombreUsuario.isBlank() || roles.isEmpty()) {
            throw new JwtException("El token no contiene una identidad válida.");
        }
        return new IdentidadJwt(usuarioId, nombreUsuario, roles, institucionId);
    }

    private SecretKey claveFirma() {
        byte[] bytes = propiedadesJwt.getSecreto().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("La clave JWT debe contener al menos 256 bits.");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    private long convertirUsuarioId(String sujeto) {
        try {
            return Long.parseLong(sujeto);
        } catch (NumberFormatException excepcion) {
            throw new JwtException("El identificador del usuario en el token no es válido.", excepcion);
        }
    }

    private List<String> convertirRoles(Object valor) {
        if (!(valor instanceof List<?> lista)) {
            throw new JwtException("El token no contiene roles válidos.");
        }
        return lista.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(rol -> List.of("user_deposito", "user_institucional", "user_tesoreria").contains(rol))
                .toList();
    }
}
