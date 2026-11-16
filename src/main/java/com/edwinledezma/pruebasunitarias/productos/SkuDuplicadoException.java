package com.edwinledezma.pruebasunitarias.productos;

import java.io.Serial;

public class SkuDuplicadoException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public SkuDuplicadoException(String sku, Throwable cause) {
        super("Ya existe un producto con el SKU " + sku, cause);
    }
}
