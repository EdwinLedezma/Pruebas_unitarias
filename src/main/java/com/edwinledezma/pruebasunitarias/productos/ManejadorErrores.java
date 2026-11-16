package com.edwinledezma.pruebasunitarias.productos;

import java.util.List;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ManejadorErrores {
    @ExceptionHandler(ProductoNoEncontradoException.class)
    ProblemDetail noEncontrado(ProductoNoEncontradoException error) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, error.getMessage());
    }

    @ExceptionHandler(SkuDuplicadoException.class)
    ProblemDetail duplicado(SkuDuplicadoException error) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, error.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validacion(MethodArgumentNotValidException error) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Revisa los campos de la solicitud.");
        List<ErrorCampo> campos = error.getBindingResult().getFieldErrors().stream()
                .map(campo -> new ErrorCampo(campo.getField(), campo.getDefaultMessage()))
                .toList();
        problema.setProperty("errores", campos);
        return problema;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail jsonInvalido() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "El cuerpo JSON es inválido.");
    }

    @ExceptionHandler(DataAccessResourceFailureException.class)
    ProblemDetail baseNoDisponible() {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE, "La base de datos no está disponible.");
    }

    record ErrorCampo(String campo, String mensaje) {
    }
}
