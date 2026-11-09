package com.edwinledezma.pruebasunitarias.productos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository repository;

    @InjectMocks
    private ProductoService service;

    @Test
    void findAllDevuelveLosProductosDelRepositorio() {
        Producto teclado = new Producto("Teclado", new BigDecimal("49.90"));
        Producto raton = new Producto("Ratón", new BigDecimal("24.50"));
        when(repository.findAll()).thenReturn(List.of(teclado, raton));

        List<Producto> resultado = service.findAll();

        assertThat(resultado)
                .extracting(Producto::getNombre, Producto::getPrecio)
                .containsExactly(tuple("Teclado", new BigDecimal("49.90")),
                        tuple("Ratón", new BigDecimal("24.50")));
        verify(repository).findAll();
    }

    @Test
    void findAllDevuelveUnaListaVaciaSiNoHayProductos() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.findAll()).isEmpty();
        verify(repository).findAll();
    }

    @Test
    void createGuardaLosAtributosYDevuelveElProductoConId() {
        String nombre = "Monitor";
        BigDecimal precio = new BigDecimal("149.99");
        Producto guardado = new Producto(nombre, precio);
        ReflectionTestUtils.setField(guardado, "id", 42L);
        when(repository.save(any(Producto.class))).thenReturn(guardado);

        Producto resultado = service.create(nombre, precio);

        ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);
        verify(repository).save(captor.capture());
        Producto enviado = captor.getValue();
        assertThat(enviado.getId()).isNull();
        assertThat(enviado.getNombre()).isEqualTo(nombre);
        assertThat(enviado.getPrecio()).isEqualByComparingTo(precio);

        assertThat(resultado.getId()).isEqualTo(42L);
        assertThat(resultado.getNombre()).isEqualTo(nombre);
        assertThat(resultado.getPrecio()).isEqualByComparingTo(precio);
    }
}
