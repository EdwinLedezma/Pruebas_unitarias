package com.edwinledezma.pruebasunitarias.productos;

import java.math.BigDecimal;

public record ProductoRespuesta(String id, String sku, String nombre, BigDecimal precio) {
    static ProductoRespuesta desde(Producto producto) {
        return new ProductoRespuesta(producto.id(), producto.sku(), producto.nombre(), producto.precio());
    }
}
