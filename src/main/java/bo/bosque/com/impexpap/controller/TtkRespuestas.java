package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.utils.ApiResponse;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * Respuestas HTTP del módulo Tickets / Bingo: 200 con datos, 204 sin datos (sin texto) y 201 en una
 * escritura con el mensaje del SP. Los errores de negocio los traduce GlobalExceptionHandler (400).
 */
final class TtkRespuestas {

    static final String EXITO = "Operación realizada exitosamente";

    private TtkRespuestas() {
    }

    static ResponseEntity<ApiResponse<?>> listado(List<?> filas) {
        if (filas == null || filas.isEmpty()) return sinDatos();
        return ResponseEntity.ok(new ApiResponse<>(EXITO, filas, HttpStatus.OK.value()));
    }

    static ResponseEntity<ApiResponse<?>> uno(Object dato) {
        if (dato == null) return sinDatos();
        return ResponseEntity.ok(new ApiResponse<>(EXITO, dato, HttpStatus.OK.value()));
    }

    static ResponseEntity<ApiResponse<?>> sinDatos() {
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(new ApiResponse<>(null, null, HttpStatus.NO_CONTENT.value()));
    }

    /** SpHelper ya lanzó SpBusinessException si el SP devolvió error; aquí solo llega el éxito. */
    static ResponseEntity<ApiResponse<?>> escritura(RespuestaSp res) {
        String msg = res.getErrormsg() == null || res.getErrormsg().trim().isEmpty() ? EXITO : res.getErrormsg();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(msg, res.getIdGenerado(), HttpStatus.CREATED.value()));
    }

    /** Sin clave (nula o 0) es un alta: el SP recibe I; con clave, U. */
    static boolean esAlta(Long id) {
        return id == null || id <= 0;
    }
}
