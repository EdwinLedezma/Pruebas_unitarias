package com.edwinledezma.pruebasunitarias.seguridad;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;

@Configuration
class JwtConfig {

    static final String EMISOR = "pruebas-unitarias";
    static final String AUDIENCIA = "productos-api";
    static final Duration VIGENCIA = Duration.ofMinutes(15);

    @Bean
    SecretKey jwtSigningKey() throws NoSuchAlgorithmException {
        var generator = KeyGenerator.getInstance("HmacSHA256");
        generator.init(256);
        return generator.generateKey();
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSigningKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSigningKey) {
        var decoder = NimbusJwtDecoder.withSecretKey(jwtSigningKey).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtIssuerValidator(EMISOR),
                new JwtClaimValidator<List<String>>("aud", audience -> audience != null && audience.contains(AUDIENCIA)),
                new JwtClaimValidator<Instant>("exp", Objects::nonNull),
                new JwtTimestampValidator(Duration.ZERO)));
        return decoder;
    }
}
