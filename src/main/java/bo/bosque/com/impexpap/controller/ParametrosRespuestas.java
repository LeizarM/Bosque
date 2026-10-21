package bo.bosque.com.impexpap.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import bo.bosque.com.impexpap.utils.ApiResponse;
import bo.bosque.com.impexpap.utils.RespuestaSp;

/**
 * Respuestas de las pantallas de parámetros: 200 con datos, 204 sin datos y 201 en una escritura.
 * El texto de éxito lo escribe el SP en {@code @errormsg}; los errores ya los tradujo SpHelper (400).
 */
final class ParametrosRespuestas {

    static final String EXITO = "Operación realizada exitosamente";

    private ParametrosRespuestas() {
    }

    static ResponseEntity<ApiResponse<?>> listado(List<?> filas) {
        if (filas == null || filas.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(new ApiResponse<>(null, null, HttpStatus.NO_CONTENT.value()));
        }
        return ResponseEntity.ok(new ApiResponse<>(EXITO, filas, HttpStatus.OK.value()));
    }

    static ResponseEntity<ApiResponse<?>> escritura(RespuestaSp res) {
        String msg = res.getErrormsg() == null || res.getErrormsg().trim().isEmpty() ? EXITO : res.getErrormsg();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(msg, res.getIdGenerado(), HttpStatus.CREATED.value()));
    }
}
