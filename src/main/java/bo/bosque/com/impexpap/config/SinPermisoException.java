package bo.bosque.com.impexpap.config;

import org.springframework.security.access.AccessDeniedException;

/**
 * Un {@code 403} <b>con un mensaje para el usuario</b>.
 *
 * <p>{@link AccessDeniedException} a secas termina en {@code GlobalExceptionHandler} con un texto fijo ("No tienes
 * los permisos necesarios para realizar esta acción") sin importar el motivo: no dice que permiso falta ni que
 * puede hacer el usuario. Esta subclase conserva el codigo 403 (semanticamente es un permiso, no un dato mal
 * escrito) pero el manejador devuelve SU mensaje. Un {@code AccessDeniedException} cualquiera sigue respondiendo
 * el texto fijo de siempre: no cambia nada para los demas modulos.
 *
 * <p>El mensaje se muestra tal cual al usuario: tiene que decir <i>que</i> no puede hacer, <i>por que</i> y <i>que
 * hacer</i>, sin datos internos mas alla del nombre del boton (util para quien administra los permisos).
 */
public class SinPermisoException extends AccessDeniedException {

    public SinPermisoException(String mensaje) {
        super(mensaje);
    }
}
