package com.edwinledezma.pruebasunitarias.productos;

import java.math.BigDecimal;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

@Document("productos")
public record Producto(
        @Id String id,
        @Indexed(unique = true) String sku,
        String nombre,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal precio
) {
}
