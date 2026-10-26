package com.group6.BancoAlimentos.Config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "seguridad.jwt")
public class PropiedadesJwt {

    @NotBlank
    private String secreto;

    @NotNull
    private Duration duracion = Duration.ofHours(1);

    @AssertTrue(message = "La clave JWT debe contener al menos 256 bits.")
    public boolean isSecretoConLongitudValida() {
        return secreto != null && secreto.getBytes(StandardCharsets.UTF_8).length >= 32;
    }

    @AssertTrue(message = "La duración del token JWT debe ser positiva.")
    public boolean isDuracionPositiva() {
        return duracion != null && !duracion.isZero() && !duracion.isNegative();
    }
}
