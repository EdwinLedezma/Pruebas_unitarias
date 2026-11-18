package com.edwinledezma.pruebasunitarias.seguridad;

import com.edwinledezma.pruebasunitarias.productos.ProductoController;
import com.edwinledezma.pruebasunitarias.productos.ProductoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AuthController.class, ProductoController.class})
@Import({SeguridadConfig.class, JwtConfig.class})
class SeguridadHttpTest {

    private static final String LOGIN = "/api/auth/login";
    private static final String PRODUCTOS = "/api/productos";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private JwtEncoder encoder;
    @MockitoBean private ProductoService service;

    @DynamicPropertySource
    static void credenciales(DynamicPropertyRegistry properties) {
        UsuariosDePrueba.registrar(properties);
    }

    @Test
    void deniegaConsultaSinToken() throws Exception {
        mvc.perform(get(PRODUCTOS))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));
        verifyNoInteractions(service);
    }

    @Test
    void deniegaTokenMalformado() throws Exception {
        mvc.perform(get(PRODUCTOS).header(HttpHeaders.AUTHORIZATION, "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void deniegaFirmaAlteradaYTokensVencidos() throws Exception {
        String token = autenticar("lector", UsuariosDePrueba.LECTOR);
        int signature = token.lastIndexOf('.') + 1;
        String alterado = token.substring(0, signature)
                + (token.charAt(signature) == 'A' ? "B" : "A") + token.substring(signature + 1);

        assertDenegado(alterado);
        assertDenegado(firmar(JwtConfig.EMISOR, List.of(JwtConfig.AUDIENCIA), Instant.now().minusSeconds(1)));
        verifyNoInteractions(service);
    }

    @Test
    void deniegaEmisorAudienciaYExpiracionIncorrectos() throws Exception {
        Instant vigente = Instant.now().plusSeconds(90);
        assertDenegado(firmar("emisor-ajeno", List.of(JwtConfig.AUDIENCIA), vigente));
        assertDenegado(firmar(JwtConfig.EMISOR, List.of("otra-api"), vigente));
        assertDenegado(firmar(JwtConfig.EMISOR, null, vigente));
        assertDenegado(firmar(JwtConfig.EMISOR, List.of(JwtConfig.AUDIENCIA), null));
        verifyNoInteractions(service);
    }

    @Test
    void lectorPuedeConsultarPeroNoCrear() throws Exception {
        String token = autenticar("lector", UsuariosDePrueba.LECTOR);
        mvc.perform(get(PRODUCTOS).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
        mvc.perform(post(PRODUCTOS).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginRechazaCredencialesIncorrectasYPasswordDemasiadoLargo() throws Exception {
        intentarLogin("editor", "incorrecta");
        intentarLogin("lector", "á".repeat(40));
    }

    @Test
    void rechazaConfiguracionInsegura() {
        var config = new SeguridadConfig();
        assertThatThrownBy(() -> config.authenticationManager(" ", UsuariosDePrueba.EDITOR))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> config.authenticationManager("breve", UsuariosDePrueba.EDITOR))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> config.authenticationManager("á".repeat(40), UsuariosDePrueba.EDITOR))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> config.authenticationManager(UsuariosDePrueba.EDITOR, UsuariosDePrueba.EDITOR))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private void intentarLogin(String usuario, String password) throws Exception {
        mvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("usuario", usuario, "password", password))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.access_token").doesNotExist());
    }

    private String autenticar(String usuario, String password) throws Exception {
        String body = mvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("usuario", usuario, "password", password))))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(cookie().doesNotExist("JSESSIONID"))
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").value(900))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).path("access_token").asText();
    }

    private void assertDenegado(String token) throws Exception {
        mvc.perform(get(PRODUCTOS).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    private String firmar(String issuer, List<String> audience, Instant expiration) {
        var claims = JwtClaimsSet.builder().issuer(issuer).subject("lector").claim("roles", List.of("LECTOR"));
        if (audience != null) {
            claims.audience(audience);
        }
        if (expiration != null) {
            claims.expiresAt(expiration);
        }
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims.build()))
                .getTokenValue();
    }
}
