package com.edwinledezma.pruebasunitarias.productos;

import com.edwinledezma.pruebasunitarias.seguridad.UsuariosDePrueba;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.mongodb.client.model.Filters.eq;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductoApiIT {

    private static final String API = "/api/productos";
    private static final String DB = "productos_it_" + UUID.randomUUID().toString().replace("-", "");

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private MongoTemplate mongo;

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry properties) {
        UsuariosDePrueba.registrar(properties);
        properties.add("spring.data.mongodb.uri", () -> System.getenv().getOrDefault(
                "MONGODB_TEST_URI", "mongodb://127.0.0.1:27017/?serverSelectionTimeoutMS=5000"));
        properties.add("spring.data.mongodb.database", () -> DB);
    }

    @BeforeEach
    void autenticarEditor() {
        http.getRestTemplate().setInterceptors(List.of());
        var login = http.postForEntity("/api/auth/login",
                Map.of("usuario", "editor", "password", UsuariosDePrueba.EDITOR), JsonNode.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = login.getBody().path("access_token").asText();
        http.getRestTemplate().setInterceptors(List.of((request, body, execution) -> {
            request.getHeaders().setBearerAuth(token);
            return execution.execute(request, body);
        }));
    }

    @AfterAll
    static void limpiarBaseDePruebas(@Autowired MongoTemplate mongo) {
        mongo.getDb().drop();
    }

    @Test
    void altaConsultaActualizacionYEliminacionSobreMongoDb() {
        ProductoSolicitud original = solicitud("CRUD-01", "Monitor", "149.99");
        ResponseEntity<JsonNode> alta = http.postForEntity(API, original, JsonNode.class);
        assertThat(alta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String id = alta.getBody().path("id").asText();
        assertThat(id).matches("[a-f0-9]{24}");
        String ruta = API + "/" + id;
        assertThat(alta.getHeaders().getLocation()).hasToString(ruta);

        ResponseEntity<JsonNode> consulta = http.getForEntity(ruta, JsonNode.class);
        assertThat(consulta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(consulta.getBody()).isEqualTo(alta.getBody());
        assertThat(http.getForEntity(API, String.class).getBody()).contains(id);

        ProductoSolicitud cambio = solicitud("CRUD-02", "Monitor 4K", "249.95");
        ResponseEntity<JsonNode> edicion = http.exchange(ruta, HttpMethod.PUT,
                new HttpEntity<>(cambio), JsonNode.class);
        assertThat(edicion.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(edicion.getBody().path("id").asText()).isEqualTo(id);
        assertThat(edicion.getBody().path("sku").asText()).isEqualTo(cambio.sku());
        assertThat(edicion.getBody().path("nombre").asText()).isEqualTo(cambio.nombre());
        assertThat(edicion.getBody().path("precio").decimalValue()).isEqualByComparingTo(cambio.precio());

        var persistido = mongo.getCollection("productos").find(eq("_id", new ObjectId(id))).first();
        assertThat(persistido).isNotNull();
        assertThat(persistido.get("precio")).isEqualTo(new Decimal128(cambio.precio()));
        assertThat(persistido.getString("sku")).isEqualTo(cambio.sku());

        ResponseEntity<Void> baja = http.exchange(ruta, HttpMethod.DELETE, HttpEntity.EMPTY, Void.class);
        assertThat(baja.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(baja.getBody()).isNull();
        assertThat(mongo.getCollection("productos").find(eq("_id", new ObjectId(id))).first()).isNull();
        assertProblem(http.getForEntity(ruta, JsonNode.class), HttpStatus.NOT_FOUND);
    }

    @Test
    void devuelveConflictoSiElSkuYaExisteEnAltaOEdicion() {
        ProductoSolicitud primero = solicitud("DUP-01", "Teclado", "49.90");
        assertThat(http.postForEntity(API, primero, JsonNode.class).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        assertProblem(http.postForEntity(API, primero, JsonNode.class), HttpStatus.CONFLICT);

        ProductoSolicitud segundo = solicitud("DUP-02", "Ratón", "24.50");
        ResponseEntity<JsonNode> alta = http.postForEntity(API, segundo, JsonNode.class);
        String ruta = API + "/" + alta.getBody().path("id").asText();
        assertProblem(http.exchange(ruta, HttpMethod.PUT, new HttpEntity<>(primero), JsonNode.class),
                HttpStatus.CONFLICT);
        assertThat(http.getForEntity(ruta, JsonNode.class).getBody().path("sku").asText())
                .isEqualTo(segundo.sku());
    }

    @Test
    void devuelve400YDetallesParaCamposInvalidosYJsonIncorrecto() {
        var invalido = solicitud("minúsculas", " ", "-1");
        var validacion = http.postForEntity(API, invalido, JsonNode.class);
        assertProblem(validacion, HttpStatus.BAD_REQUEST);
        assertThat(validacion.getBody().path("errores").findValuesAsText("campo"))
                .contains("sku", "nombre", "precio");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        assertProblem(http.postForEntity(API, new HttpEntity<>("{", headers), JsonNode.class),
                HttpStatus.BAD_REQUEST);
    }

    @Test
    void devuelve404AlEditarOEliminarUnProductoAusente() {
        String ruta = API + "/" + new ObjectId();
        assertProblem(http.exchange(ruta, HttpMethod.PUT,
                new HttpEntity<>(solicitud("PUT-01", "Monitor", "149.99")), JsonNode.class), HttpStatus.NOT_FOUND);
        assertProblem(http.exchange(ruta, HttpMethod.DELETE, HttpEntity.EMPTY, JsonNode.class), HttpStatus.NOT_FOUND);
    }

    private static void assertProblem(ResponseEntity<JsonNode> respuesta, HttpStatus status) {
        assertThat(respuesta.getStatusCode()).isEqualTo(status);
        assertThat(respuesta.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(respuesta.getBody().path("status").asInt()).isEqualTo(status.value());
        assertThat(respuesta.getBody().path("detail").asText()).isNotBlank();
    }

    private static ProductoSolicitud solicitud(String sku, String nombre, String precio) {
        return new ProductoSolicitud(sku, nombre, new BigDecimal(precio));
    }
}
