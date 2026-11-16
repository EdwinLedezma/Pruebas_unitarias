package com.edwinledezma.pruebasunitarias.productos;

public class ProductoNoEncontradoException extends RuntimeException {
    public ProductoNoEncontradoException(String id) {
        super("No existe el producto con ID " + id);
    }
}
