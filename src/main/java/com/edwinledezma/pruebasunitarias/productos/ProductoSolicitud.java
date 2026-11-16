package com.edwinledezma.pruebasunitarias.productos;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductoSolicitud(
        @NotBlank @Pattern(regexp = "[A-Z0-9][A-Z0-9-]{0,31}") String sku,
        @NotBlank @Size(max = 120) String nombre,
        @NotNull @Positive @Digits(integer = 8, fraction = 2) BigDecimal precio
) {
}
