package com.edwinledezma.pruebasunitarias.seguridad;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
class AuthController {

    private final AuthenticationManager authentication;
    private final JwtEncoder encoder;

    AuthController(AuthenticationManager authentication, JwtEncoder encoder) {
        this.authentication = authentication;
        this.encoder = encoder;
    }

    @PostMapping("/login")
    ResponseEntity<TokenRespuesta> login(@Valid @RequestBody LoginSolicitud solicitud) {
        if (solicitud.password().getBytes(StandardCharsets.UTF_8).length > SeguridadConfig.MAX_PASSWORD_BYTES) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            var usuario = authentication.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(
                    solicitud.usuario(), solicitud.password()));
            var roles = usuario.getAuthorities().stream()
                    .map(authority -> authority.getAuthority().substring("ROLE_".length()))
                    .toList();
            Instant ahora = Instant.now();
            var claims = JwtClaimsSet.builder()
                    .issuer(JwtConfig.EMISOR)
                    .audience(List.of(JwtConfig.AUDIENCIA))
                    .subject(usuario.getName())
                    .issuedAt(ahora)
                    .expiresAt(ahora.plus(JwtConfig.VIGENCIA))
                    .claim("roles", roles)
                    .build();
            var header = JwsHeader.with(MacAlgorithm.HS256).build();
            var token = encoder.encode(JwtEncoderParameters.from(header, claims));
            return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                    .body(new TokenRespuesta(token.getTokenValue(), "Bearer", JwtConfig.VIGENCIA.toSeconds()));
        } catch (AuthenticationException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    record LoginSolicitud(@NotBlank @Size(max = 32) String usuario,
                          @NotBlank @Size(max = SeguridadConfig.MAX_PASSWORD_BYTES) String password) {
    }

    record TokenRespuesta(@JsonProperty("access_token") String accessToken,
                          @JsonProperty("token_type") String tokenType,
                          @JsonProperty("expires_in") long expiresIn) {
    }
}
