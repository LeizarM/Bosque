package bo.bosque.com.impexpap.config;

import bo.bosque.com.impexpap.utils.ApiResponse;
import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 1. Errores de Negocio (SQL) -> 400 Bad Request
    @ExceptionHandler(SpBusinessException.class)
    public ResponseEntity<ApiResponse<?>> handleSpBusinessException(SpBusinessException ex) {
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(ex.getMessage(), null, HttpStatus.BAD_REQUEST.value()));
    }

    // 1.a Lo mismo, pero el usuario PUEDE confirmarlo -> 400 con "confirmable": true
    //     Va antes que el handler de SpBusinessException por claridad; Spring elige igual el
    //     @ExceptionHandler del tipo más específico, no el primero declarado.
    @ExceptionHandler(SpConfirmableException.class)
    public ResponseEntity<ApiResponse<?>> handleSpConfirmableException(SpConfirmableException ex) {
        ApiResponse<?> cuerpo =
                new ApiResponse<>(ex.getMessage(), null, HttpStatus.BAD_REQUEST.value());
        // El marcador que le dice al cliente "ofrecé Confirmar y reintentá con confirmado:true",
        // en vez de obligarlo a buscar una palabra en el mensaje. Ver SpConfirmableException.
        cuerpo.setConfirmable(Boolean.TRUE);
        return ResponseEntity.badRequest().body(cuerpo);
    }

    // 1.b Datos del ERP en un estado que impide responder -> 409 Conflict
    //     No es un 400: la pregunta está bien hecha, el que está mal es el dato, y el cliente
    //     no lo arregla reintentando. Ver SpConflictException.
    @ExceptionHandler(SpConflictException.class)
    public ResponseEntity<ApiResponse<?>> handleSpConflictException(SpConflictException ex) {
        logger.warn("Conflicto de datos: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiResponse<>(ex.getMessage(), null, HttpStatus.CONFLICT.value()));
    }

    // 2. Errores de Permisos (Spring Security) -> 403 Forbidden
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccessDeniedException(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse<>("No tienes los permisos necesarios para realizar esta acción.", null, HttpStatus.FORBIDDEN.value()));
    }

    // 3. Solicitud multipart malformada o sin Content-Type correcto -> 400 Bad Request
    //    Ocurre cuando el cliente llama a un endpoint de subida de archivos sin enviar
    //    Content-Type: multipart/form-data o sin incluir el campo 'file'.
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiResponse<?>> handleMultipartException(MultipartException ex) {
        logger.warn("Solicitud multipart inválida: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(
                        "La solicitud debe ser multipart/form-data. Asegúrate de enviar el archivo en el campo 'file'.",
                        null, HttpStatus.BAD_REQUEST.value()));
    }

    // 4. Content-Type no soportado (ej: llamar endpoint multipart sin header Content-Type) -> 415
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<?>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        logger.warn("Content-Type no soportado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(new ApiResponse<>(
                        "Content-Type no soportado. Para subir archivos usa 'multipart/form-data' con los campos 'file' (File) y 'audUsuario' (Text).",
                        null, HttpStatus.UNSUPPORTED_MEDIA_TYPE.value()));
    }

    // 5. Parámetro de query requerido ausente -> 400 Bad Request
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<?>> handleMissingParam(MissingServletRequestParameterException ex) {
        logger.warn("Parámetro requerido ausente: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(
                        "Parámetro requerido ausente: '" + ex.getParameterName() + "'.",
                        null, HttpStatus.BAD_REQUEST.value()));
    }

    /**
     * El body no se pudo leer: JSON roto, o un valor que no entra en el tipo del DTO.
     *
     * <p>Sin este handler caía en el catch-all y salía un <b>500 "Error interno del
     * servidor"</b> con el stack completo en el log como "Error crítico no controlado".
     * Es engañoso: el servidor está bien, lo que vino mal es el pedido. Va 400.
     *
     * <p>El caso que lo destapó: un número de 11 dígitos en un campo {@code int} del DTO
     * ({@code Numeric value (10000000000) out of range of int}). Jackson deja el nombre
     * del campo en el mensaje, así que se extrae para que el usuario sepa cuál corregir.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<?>> handleBodyIlegible(HttpMessageNotReadableException ex) {
        String campo = campoDelError(ex);
        String detalle = ex.getMostSpecificCause().getMessage();

        logger.warn("Body ilegible{}: {}", campo == null ? "" : " (campo " + campo + ")", detalle);

        String mensaje = campo == null
                ? "El pedido tiene un formato inválido."
                : "El valor enviado en '" + campo + "' no es válido.";

        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(mensaje, null, HttpStatus.BAD_REQUEST.value()));
    }

    /** Saca el nombre del campo del "reference chain" que arma Jackson, si está. */
    private String campoDelError(HttpMessageNotReadableException ex) {
        Throwable causa = ex.getCause();
        if (causa instanceof JsonMappingException) {
            List<JsonMappingException.Reference> ruta = ((JsonMappingException) causa).getPath();
            if (ruta != null && !ruta.isEmpty()) {
                return ruta.get(ruta.size() - 1).getFieldName();
            }
        }
        return null;
    }

    /**
     * 5.a El cliente cortó la conexión. NO es un error del servidor y no se responde nada.
     *
     * <h3>Qué es</h3>
     * Tomcat lanza {@link ClientAbortException} cuando un {@code write} sobre el socket falla
     * porque del otro lado ya no hay nadie: la app del chofer llegó a su timeout, el celular
     * perdió señal, o el usuario cerró la pantalla. Se ve como {@code Connection reset by peer}
     * o {@code Broken pipe}, y el stack apunta a Jackson —mitad de la serialización del JSON—
     * porque es justo ahí donde el servidor estaba escribiendo cuando se cayó el socket.
     *
     * <h3>Por qué devuelve void y no un ResponseEntity</h3>
     * Cuando esto salta, la respuesta ya está <b>committed</b>: los headers salieron y parte del
     * body también. Devolver un {@code ResponseEntity} hace que Spring intente serializar un 500
     * sobre ese mismo socket muerto, y el {@code write} vuelve a fallar. Eso es exactamente lo
     * que producía el segundo error apilado que se veía en producción:
     *
     * <pre>
     * ERROR GlobalExceptionHandler          : Error crítico no controlado: ... Connection reset by peer
     * WARN  ExceptionHandlerExceptionResolver : Failure in @ExceptionHandler ... Broken pipe
     * </pre>
     *
     * <p>Con {@code void}, Spring marca el request como atendido y no escribe nada.
     *
     * <h3>Por qué DEBUG y no ERROR</h3>
     * Con choferes en la calle y señal móvil, que un cliente corte es rutina, no un incidente.
     * En ERROR tapaba los errores de verdad.
     */
    @ExceptionHandler(ClientAbortException.class)
    public void handleClientAbort(ClientAbortException ex) {
        logger.debug("El cliente cortó la conexión antes de recibir la respuesta completa: {}",
                     ex.getMessage());
    }

    /**
     * 6. Fallos Críticos No Controlados -> 500 Internal Server Error.
     *
     * <p>Con dos salvaguardas antes de intentar responder, las dos por el mismo motivo: no tiene
     * sentido escribir un cuerpo de error en una respuesta que ya no lo admite.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleAllUncaughtException(Exception ex, HttpServletResponse response) {

        // (a) El corte del cliente no siempre llega como ClientAbortException pelada: si alguna
        //     capa intermedia la envuelve, el handler específico de arriba no se elige y termina
        //     cayendo acá. Se recorre la cadena de causas y se trata igual.
        if (esCorteDelCliente(ex)) {
            logger.debug("El cliente cortó la conexión antes de recibir la respuesta completa: {}",
                         ex.getMessage());
            return null;
        }

        logger.error("Error crítico no controlado: ", ex);

        // (b) Si la respuesta ya está committed, el status y los headers ya viajaron: no se puede
        //     convertir en 500 lo que el cliente ya está leyendo como 200. Intentar escribir el
        //     cuerpo de error solo agrega un IOException arriba del error real. Pasa cuando algo
        //     falla a mitad de la serialización (un getter que lanza, por ejemplo).
        if (response.isCommitted()) {
            logger.warn("La respuesta ya estaba committed cuando saltó la excepción; no se puede enviar "
                      + "el 500 y se cierra tal cual. El cliente va a ver un JSON truncado.");
            return null;
        }

        // Los dos returns de arriba son legales y no producen una respuesta vacía inesperada.
        // ExceptionHandlerExceptionResolver arma su ModelAndViewContainer con requestHandled=true,
        // así que ServletInvocableHandlerMethod.invokeAndHandle() ve el null y corta antes de
        // llamar a ningún return value handler: no se escribe nada en el socket. En ambos casos
        // ese socket ya estaba comprometido de todos modos.
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>("Error interno del servidor.", null, HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }

    /**
     * ¿Esta excepción, o alguna de sus causas, es en realidad "el cliente se fue"?
     *
     * <p>Se recorre la cadena entera y no solo la excepción de arriba porque el corte suele venir
     * envuelto. Además del tipo de Tomcat se miran los mensajes clásicos del sistema operativo: un
     * {@link IOException} con {@code Broken pipe} o {@code Connection reset by peer} es lo mismo
     * aunque no haya pasado por {@link ClientAbortException}.
     *
     * <p>El recorrido está acotado contra una cadena de causas cíclica (rara, pero existe), que
     * colgaría un bucle ingenuo justo cuando algo ya salió mal.
     */
    private boolean esCorteDelCliente(Throwable t) {
        for (Throwable causa = t; causa != null && causa != causa.getCause(); causa = causa.getCause()) {
            if (causa instanceof ClientAbortException) {
                return true;
            }
            String mensaje = causa.getMessage();
            if (causa instanceof IOException && mensaje != null
                    && (mensaje.contains("Broken pipe") || mensaje.contains("Connection reset by peer"))) {
                return true;
            }
        }
        return false;
    }
}