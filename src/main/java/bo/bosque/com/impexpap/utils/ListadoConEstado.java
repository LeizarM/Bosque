// Clase de bo.bosque.com.impexpap.utils — companero de SpHelper.ejecutarListadoConEstado
package bo.bosque.com.impexpap.utils;

import java.util.List;

/**
 * Resultado de un SP que SIEMPRE produce exactamente 2 resultsets: (1) las
 * filas del listado y (2) una única fila de estado (autorizado, error,
 * errormsg). Ver {@link SpHelper#ejecutarListadoConEstado}.
 *
 * <p>Se creó para p_list_tac_dependientesJefe: ni ejecutarListado (solo lee
 * filas) ni ejecutarAbmMap (solo lee un SELECT final fijo de 3 columnas y
 * descarta cualquier otro resultset) sirven para esta forma mixta —
 * mezclar un resultset real con parámetros OUTPUT verdaderos no es
 * confiable con jTDS, así que el SP devuelve el estado como un segundo
 * resultset en vez de OUTPUT.
 */
public class ListadoConEstado<T> {

    private final List<T> filas;
    private final boolean autorizado;
    private final int error;
    private final String errormsg;

    public ListadoConEstado(List<T> filas, boolean autorizado, int error, String errormsg) {
        this.filas = filas;
        this.autorizado = autorizado;
        this.error = error;
        this.errormsg = errormsg;
    }

    public List<T> getFilas() {
        return filas;
    }

    public boolean isAutorizado() {
        return autorizado;
    }

    public int getError() {
        return error;
    }

    public String getErrormsg() {
        return errormsg;
    }
}
