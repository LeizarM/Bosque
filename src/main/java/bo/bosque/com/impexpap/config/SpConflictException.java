package bo.bosque.com.impexpap.config;

/**
 * Los datos del ERP están en un estado que impide responder, y <b>no es culpa de quien preguntó</b>:
 * hay que arreglarlos antes de volver a consultar. Se traduce en un <b>409 Conflict</b>
 * ({@link GlobalExceptionHandler}).
 *
 * <p><b>Por qué no alcanzaba con {@link SpBusinessException}.</b> Esa está mapeada a 400 y sólo a
 * 400 — "mandaste mal la pregunta". Acá la pregunta está bien: el que está mal es el dato. La
 * diferencia importa porque el cliente no puede corregir un 409 reintentando con otros
 * parámetros, y porque un 400 en el log de RR.HH. se lee como "el módulo nuevo falla" en vez de
 * "hay un empleado con dos contratos activos".
 *
 * <p><b>Caso que la estrenó:</b> {@code p_list_Permiso @ACCION='C'} y {@code 'D'} unen a
 * {@code tb_relEmplEmpr ... esActivo=1} <b>sin {@code TOP(1)}</b>, así que un empleado con dos
 * relaciones activas devuelve dos filas con dos saldos distintos. Elegir una en silencio sería
 * inventar un número; se corta y se dice qué arreglar. Hoy 0 empleados están en ese estado
 * <i>(verificado)</i> — es una puerta defensiva, no un bug conocido.
 */
public class SpConflictException extends RuntimeException {

    public SpConflictException(String message) {
        super(message);
    }
}
