package com.edwinledezma.pruebasunitarias.productos;

public class SkuDuplicadoException extends RuntimeException {
    public SkuDuplicadoException(String sku, Throwable cause) {
        super("Ya existe un producto con el SKU " + sku, cause);
    }
}
