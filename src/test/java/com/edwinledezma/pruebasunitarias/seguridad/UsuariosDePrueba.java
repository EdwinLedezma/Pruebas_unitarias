package com.edwinledezma.pruebasunitarias.seguridad;

import java.util.UUID;
import org.springframework.test.context.DynamicPropertyRegistry;

public final class UsuariosDePrueba {

    public static final String LECTOR = UUID.randomUUID().toString();
    public static final String EDITOR = UUID.randomUUID().toString();

    private UsuariosDePrueba() {
    }

    public static void registrar(DynamicPropertyRegistry properties) {
        properties.add("app.auth.lector-password", () -> LECTOR);
        properties.add("app.auth.editor-password", () -> EDITOR);
    }
}
