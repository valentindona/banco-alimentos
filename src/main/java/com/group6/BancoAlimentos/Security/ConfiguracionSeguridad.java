package com.group6.BancoAlimentos.Security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class ConfiguracionSeguridad {

    @Bean
    SecurityFilterChain filtroSeguridad(
            HttpSecurity http,
            FiltroJwt filtroJwt,
            ManejadorErroresSeguridad manejadorErrores) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint((solicitud, respuesta, excepcion) ->
                                manejadorErrores.escribir(solicitud, respuesta, HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler((solicitud, respuesta, excepcion) ->
                                manejadorErrores.escribir(solicitud, respuesta, HttpStatus.FORBIDDEN)))
                .authorizeHttpRequests(autorizacion -> autorizacion
                        .requestMatchers("/api/auth/login", "/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/usuarios").hasRole("user_deposito")
                        .requestMatchers(HttpMethod.POST, "/api/donantes", "/api/productos", "/api/donaciones")
                        .hasRole("user_deposito")
                        .requestMatchers(HttpMethod.GET, "/api/donantes", "/api/donantes/**",
                                "/api/donaciones", "/api/donaciones/**")
                        .hasRole("user_deposito")
                        .requestMatchers(HttpMethod.PUT, "/api/donantes/**", "/api/productos/**")
                        .hasRole("user_deposito")
                        .requestMatchers(HttpMethod.DELETE, "/api/donantes/**", "/api/productos/**")
                        .hasRole("user_deposito")
                        .requestMatchers("/api/remitos", "/api/remitos/**")
                        .hasRole("user_deposito")
                        .requestMatchers(HttpMethod.GET, "/api/productos", "/api/productos/**")
                        .hasRole("user_deposito")
                        .requestMatchers(HttpMethod.GET, "/api/stock", "/api/stock/**")
                        .hasRole("user_deposito")
                        .requestMatchers(HttpMethod.POST, "/api/ajustes")
                        .hasRole("user_deposito")
                        .requestMatchers(HttpMethod.PUT, "/api/ajustes/**")
                        .hasRole("user_deposito")
                        .requestMatchers(HttpMethod.DELETE, "/api/ajustes/**")
                        .hasRole("user_deposito")
                        .requestMatchers(HttpMethod.GET, "/api/ajustes")
                        .hasRole("user_deposito")
                        .requestMatchers(HttpMethod.POST, "/api/instituciones")
                        .hasRole("user_institucional")
                        .requestMatchers(HttpMethod.PUT, "/api/instituciones/**")
                        .hasRole("user_institucional")
                        .requestMatchers(HttpMethod.DELETE, "/api/instituciones/**")
                        .hasRole("user_institucional")
                        .requestMatchers(HttpMethod.PATCH, "/api/instituciones/*/estado-pago")
                        .hasRole("user_tesoreria")
                        .requestMatchers(HttpMethod.GET, "/api/instituciones", "/api/instituciones/**")
                        .hasAnyRole("user_deposito", "user_tesoreria", "user_institucional")
                        .requestMatchers(HttpMethod.GET, "/api/facturas", "/api/facturas/**")
                        .hasRole("user_deposito")
                        .requestMatchers(HttpMethod.POST, "/api/facturas", "/api/facturas/**")
                        .hasRole("user_deposito")
                        .requestMatchers(HttpMethod.PUT, "/api/facturas/**")
                        .hasRole("user_deposito")
                        .requestMatchers(HttpMethod.DELETE, "/api/facturas/**")
                        .hasRole("user_deposito")
                        .requestMatchers(HttpMethod.GET, "/api/notificaciones")
                        .hasRole("user_deposito")
                        .requestMatchers("/api/auth/yo").authenticated()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .addFilterBefore(filtroJwt, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
