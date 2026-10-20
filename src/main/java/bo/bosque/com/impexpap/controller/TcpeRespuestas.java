package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.utils.ApiResponse;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * Respuestas HTTP del módulo de exámenes: 200 con datos, 204 sin datos (sin texto: un 204 no lleva cuerpo) y 201 en
 * una escritura, con el mensaje que da el SP. Los errores de negocio los traduce {@code GlobalExceptionHandler} (400).
 */
final class TcpeRespuestas {

    static final String EXITO = "Operación realizada exitosamente";

    private TcpeRespuestas() {
    }

    static ResponseEntity<ApiResponse<?>> listado(List<?> filas) {
        if (filas == null || filas.isEmpty()) return sinDatos();
        return ResponseEntity.ok(new ApiResponse<>(EXITO, filas, HttpStatus.OK.value()));
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

    static ResponseEntity<ApiResponse<?>> ok(Object data) {
        return ResponseEntity.ok(new ApiResponse<>(EXITO, data, HttpStatus.OK.value()));
    }

    static ResponseEntity<ApiResponse<?>> creado(Object data) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(EXITO, data, HttpStatus.CREATED.value()));
    }

    static ResponseEntity<ApiResponse<?>> error(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status).body(new ApiResponse<>(mensaje, null, status.value()));
    }

    /** {@code application/pdf} en línea; sin bytes (nada que imprimir), 204. */
    static ResponseEntity<byte[]> pdf(byte[] bytes, String archivo) {
        if (bytes == null || bytes.length == 0) return ResponseEntity.noContent().build();
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_PDF);
        h.setContentLength(bytes.length);
        h.setContentDisposition(ContentDisposition.inline().filename(archivo).build());
        return new ResponseEntity<>(bytes, h, HttpStatus.OK);
    }
}
