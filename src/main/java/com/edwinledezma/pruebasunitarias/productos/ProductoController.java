package com.edwinledezma.pruebasunitarias.productos;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {
    private final ProductoService service;

    public ProductoController(ProductoService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProductoRespuesta> findAll() {
        return service.findAll().stream().map(ProductoRespuesta::desde).toList();
    }

    @GetMapping("/{id}")
    public ProductoRespuesta findById(@PathVariable String id) {
        return ProductoRespuesta.desde(service.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ProductoRespuesta> create(@Valid @RequestBody ProductoSolicitud solicitud) {
        Producto producto = service.create(solicitud);
        return ResponseEntity.created(URI.create("/api/productos/" + producto.id())).body(ProductoRespuesta.desde(producto));
    }

    @PutMapping("/{id}")
    public ProductoRespuesta update(@PathVariable String id, @Valid @RequestBody ProductoSolicitud solicitud) {
        return ProductoRespuesta.desde(service.update(id, solicitud));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        service.delete(id);
    }
}
