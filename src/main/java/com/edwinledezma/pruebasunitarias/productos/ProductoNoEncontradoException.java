package com.edwinledezma.pruebasunitarias.productos;

import java.io.Serial;

public class ProductoNoEncontradoException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public ProductoNoEncontradoException(String id) {
        super("No existe el producto con ID " + id);
    }
}
