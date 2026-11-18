package com.edwinledezma.pruebasunitarias.seguridad;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import jakarta.servlet.DispatcherType;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
class SeguridadConfig {

    private static final String PRODUCTOS = "/api/productos/**";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        var authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        var jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(authorities);

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(routes -> routes
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, PRODUCTOS).hasAnyRole("LECTOR", "EDITOR")
                        .requestMatchers(HttpMethod.POST, PRODUCTOS).hasRole("EDITOR")
                        .requestMatchers(HttpMethod.PUT, PRODUCTOS).hasRole("EDITOR")
                        .requestMatchers(HttpMethod.DELETE, PRODUCTOS).hasRole("EDITOR")
                        .anyRequest().denyAll())
                .oauth2ResourceServer(resource -> resource.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter)))
                .build();
    }

    @Bean
    AuthenticationManager authenticationManager(
            @Value("${app.auth.lector-password}") String lector,
            @Value("${app.auth.editor-password}") String editor) {
        validarPassword(lector);
        validarPassword(editor);
        if (lector.equals(editor)) {
            throw new IllegalArgumentException("Las contraseñas de las dos cuentas deben ser distintas.");
        }
        var encoder = new BCryptPasswordEncoder(12);
        var usuarios = new InMemoryUserDetailsManager(
                User.withUsername("lector").password(encoder.encode(lector)).roles("LECTOR").build(),
                User.withUsername("editor").password(encoder.encode(editor)).roles("EDITOR").build());
        var provider = new DaoAuthenticationProvider(usuarios);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    private static void validarPassword(String password) {
        if (password.isBlank() || password.length() < 16 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Cada contraseña debe tener al menos 16 caracteres y hasta 72 bytes UTF-8.");
        }
    }
}
