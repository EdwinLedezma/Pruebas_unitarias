package com.edwinledezma.pruebasunitarias.productos;

import com.mongodb.client.result.DeleteResult;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.UpdateDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    private static final ProductoSolicitud SOLICITUD =
            new ProductoSolicitud("MON-01", "Monitor", new BigDecimal("149.99"));
    private static final Producto NUEVO =
            new Producto(null, SOLICITUD.sku(), SOLICITUD.nombre(), SOLICITUD.precio());
    private static final Producto GUARDADO =
            new Producto("id-1", SOLICITUD.sku(), SOLICITUD.nombre(), SOLICITUD.precio());

    @Mock
    private ProductoRepository repository;

    @Mock
    private MongoOperations mongo;

    @InjectMocks
    private ProductoService service;

    @Test
    void findAllDevuelveLosProductosDelRepositorio() {
        when(repository.findAll()).thenReturn(List.of(GUARDADO));

        assertThat(service.findAll()).containsExactly(GUARDADO);
    }

    @Test
    void findAllDevuelveUnaListaVaciaSiNoHayProductos() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.findAll()).isEmpty();
    }

    @Test
    void findByIdDevuelveElProductoExistente() {
        when(repository.findById(GUARDADO.id())).thenReturn(Optional.of(GUARDADO));

        assertThat(service.findById(GUARDADO.id())).isEqualTo(GUARDADO);
    }

    @Test
    void findByIdInformaSiElProductoNoExiste() {
        when(repository.findById("ausente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById("ausente"))
                .isInstanceOf(ProductoNoEncontradoException.class).hasMessageContaining("ausente");
    }

    @Test
    void createInsertaLosAtributosYDevuelveElProductoConId() {
        when(repository.insert(NUEVO)).thenReturn(GUARDADO);

        assertThat(service.create(SOLICITUD)).isEqualTo(GUARDADO);
        verify(repository).insert(NUEVO);
    }

    @Test
    void createTraduceLaClaveDuplicada() {
        var error = new DuplicateKeyException("sku");
        when(repository.insert(NUEVO)).thenThrow(error);

        assertThatThrownBy(() -> service.create(SOLICITUD))
                .isInstanceOf(SkuDuplicadoException.class)
                .hasMessageContaining(SOLICITUD.sku())
                .hasCause(error);
    }

    @Test
    void updateInformaSiElProductoNoExiste() {
        assertThatThrownBy(() -> service.update("ausente", SOLICITUD))
                .isInstanceOf(ProductoNoEncontradoException.class).hasMessageContaining("ausente");
    }

    @Test
    void updateTraduceLaClaveDuplicada() {
        when(mongo.findAndModify(any(Query.class), any(UpdateDefinition.class),
                any(FindAndModifyOptions.class), eq(Producto.class)))
                .thenThrow(new DuplicateKeyException("sku"));

        assertThatThrownBy(() -> service.update(GUARDADO.id(), SOLICITUD))
                .isInstanceOf(SkuDuplicadoException.class).hasMessageContaining(SOLICITUD.sku());
    }

    @Test
    void deleteInformaSiNoSeEliminoNingunDocumento() {
        when(mongo.remove(any(Query.class), eq(Producto.class))).thenReturn(DeleteResult.acknowledged(0));

        assertThatThrownBy(() -> service.delete("ausente"))
                .isInstanceOf(ProductoNoEncontradoException.class).hasMessageContaining("ausente");
    }
}
