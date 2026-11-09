package com.edwinledezma.pruebasunitarias.productos;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProductoService {

    private final ProductoRepository repository;

    public ProductoService(ProductoRepository repository) {
        this.repository = repository;
    }

    public List<Producto> findAll() {
        return repository.findAll();
    }

    public Producto create(String nombre, BigDecimal precio) {
        return repository.save(new Producto(nombre, precio));
    }
}
