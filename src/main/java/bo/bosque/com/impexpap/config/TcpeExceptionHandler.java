package bo.bosque.com.impexpap.config;

import bo.bosque.com.impexpap.controller.TcpeBancoController;
import bo.bosque.com.impexpap.controller.TcpeCatalogoBancoController;
import bo.bosque.com.impexpap.controller.TcpeCatalogoExamenController;
import bo.bosque.com.impexpap.controller.TcpeCatalogoPostulacionController;
import bo.bosque.com.impexpap.controller.TcpeExamenPublicoAuthController;
import bo.bosque.com.impexpap.controller.TcpeExamenPublicoController;
import bo.bosque.com.impexpap.controller.TcpeLotesController;
import bo.bosque.com.impexpap.controller.TcpeMisExamenesController;
import bo.bosque.com.impexpap.controller.TcpeResultadosController;
import bo.bosque.com.impexpap.controller.TcpeUsuariosController;
import bo.bosque.com.impexpap.utils.ApiResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 400 para el {@code @Valid} de los DTO del módulo: {@code GlobalExceptionHandler} no atiende
 * {@code MethodArgumentNotValidException} y saldría un 500. Limitado al módulo ({@code assignableTypes}) y con
 * {@code HIGHEST_PRECEDENCE} para que el {@code @ExceptionHandler(Exception.class)} global no lo tape.
 */
@RestControllerAdvice(assignableTypes = {
        TcpeBancoController.class, TcpeCatalogoBancoController.class, TcpeCatalogoExamenController.class,
        TcpeCatalogoPostulacionController.class, TcpeExamenPublicoAuthController.class,
        TcpeExamenPublicoController.class, TcpeLotesController.class, TcpeMisExamenesController.class,
        TcpeResultadosController.class, TcpeUsuariosController.class })
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TcpeExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> datosInvalidos(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(TcpeExceptionHandler::describir)
                .sorted()
                .collect(Collectors.joining(" "));
        if (mensaje.isEmpty()) mensaje = "Los datos enviados no son válidos.";
        return ResponseEntity.badRequest().body(new ApiResponse<>(mensaje, null, HttpStatus.BAD_REQUEST.value()));
    }

    private static String describir(FieldError e) {
        return e.getField() + ": " + e.getDefaultMessage();
    }
}
