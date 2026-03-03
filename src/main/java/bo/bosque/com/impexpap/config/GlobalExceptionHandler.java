package bo.bosque.com.impexpap.config;

import bo.bosque.com.impexpap.utils.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 1. Errores de Negocio (SQL) -> 400 Bad Request
    @ExceptionHandler(SpBusinessException.class)
    public ResponseEntity<ApiResponse<?>> handleSpBusinessException(SpBusinessException ex) {
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(ex.getMessage(), null, HttpStatus.BAD_REQUEST.value()));
    }

    // 2. Errores de Permisos (Spring Security) -> 403 Forbidden
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccessDeniedException(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse<>("No tienes los permisos necesarios para realizar esta acción.", null, HttpStatus.FORBIDDEN.value()));
    }

    // 3. Fallos Críticos No Controlados -> 500 Internal Server Error
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleAllUncaughtException(Exception ex) {
        logger.error("Error crítico no controlado: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>("Error interno del servidor.", null, HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }
}