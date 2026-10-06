package bo.bosque.com.impexpap.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import bo.bosque.com.impexpap.utils.ApiResponse;

/**
 * El manejador global tiene un texto FIJO para todo {@code AccessDeniedException}; {@link SinPermisoException} es el unico
 * 403 que puede decir su motivo. Estas pruebas fijan las dos mitades: el nuevo mensaje sale, y el comportamiento de los
 * demas modulos no cambia.
 */
class SinPermisoHandlerTest {

    private final GlobalExceptionHandler manejador = new GlobalExceptionHandler();

    @Test
    @DisplayName("SinPermisoException: 403 con SU mensaje (que permiso falta y que hacer)")
    void sinPermisoDiceSuMotivo() {
        String motivo = "No tienes permiso para registrar cheques. Tu usuario no tiene asignado el botón btnNuevoCH; "
                + "pídele al administrador que te lo asigne.";

        ResponseEntity<ApiResponse<?>> r = manejador.handleSinPermisoException(new SinPermisoException(motivo));

        assertEquals(HttpStatus.FORBIDDEN, r.getStatusCode());
        assertEquals(motivo, r.getBody().getMessage());
        assertNull(r.getBody().getData());
        assertEquals(403, r.getBody().getStatus());
    }

    @Test
    @DisplayName("un AccessDeniedException cualquiera (el de los demas modulos) sigue respondiendo el texto fijo de siempre")
    void elGenericoNoCambia() {
        ResponseEntity<ApiResponse<?>> r = manejador.handleAccessDeniedException(new AccessDeniedException("No tiene habilitado 'x'"));

        assertEquals(HttpStatus.FORBIDDEN, r.getStatusCode());
        assertEquals("No tienes los permisos necesarios para realizar esta acción.", r.getBody().getMessage());
    }

    @Test
    @DisplayName("SinPermisoException ES un AccessDeniedException: el codigo y los filtros de seguridad lo tratan como un 403")
    void esUnAccessDenied() {
        AccessDeniedException e = new SinPermisoException("x");
        assertEquals("x", e.getMessage());
    }
}
