package com.edwinledezma.pruebasunitarias.productos;

import java.util.List;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
public class ProductoService {
    private final ProductoRepository repository;
    private final MongoOperations mongo;

    public ProductoService(ProductoRepository repository, MongoOperations mongo) {
        this.repository = repository;
        this.mongo = mongo;
    }

    public List<Producto> findAll() {
        return repository.findAll();
    }

    public Producto findById(String id) {
        return repository.findById(id).orElseThrow(() -> new ProductoNoEncontradoException(id));
    }

    public Producto create(ProductoSolicitud solicitud) {
        try {
            return repository.insert(new Producto(null, solicitud.sku(), solicitud.nombre(), solicitud.precio()));
        } catch (DuplicateKeyException ex) {
            throw new SkuDuplicadoException(solicitud.sku(), ex);
        }
    }

    public Producto update(String id, ProductoSolicitud solicitud) {
        Update cambios = new Update()
                .set("sku", solicitud.sku())
                .set("nombre", solicitud.nombre())
                .set("precio", solicitud.precio());
        try {
            return Optional.ofNullable(mongo.findAndModify(porId(id), cambios,
                            FindAndModifyOptions.options().returnNew(true), Producto.class))
                    .orElseThrow(() -> new ProductoNoEncontradoException(id));
        } catch (DuplicateKeyException ex) {
            throw new SkuDuplicadoException(solicitud.sku(), ex);
        }
    }

    public void delete(String id) {
        if (mongo.remove(porId(id), Producto.class).getDeletedCount() == 0) {
            throw new ProductoNoEncontradoException(id);
        }
    }

    private static Query porId(String id) {
        return Query.query(Criteria.where("id").is(id));
    }
}
