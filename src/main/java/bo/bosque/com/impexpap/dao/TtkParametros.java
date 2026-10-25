package bo.bosque.com.impexpap.dao;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parámetros para SpHelper con solo los valores presentes (pares nombre, valor).
 * Un null no se envía y el SP usa su DEFAULT NULL: así no se filtra por 0 ni se pisa un dato.
 */
final class TtkParametros {

    private TtkParametros() {
    }

    static Map<String, Object> de(Object... nombreValor) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < nombreValor.length; i += 2) {
            if (nombreValor[i + 1] != null) m.put((String) nombreValor[i], nombreValor[i + 1]);
        }
        return m;
    }

    /** Alta o edición: I/U envían los datos; el resto de acciones, solo la clave. */
    static boolean esAltaOEdicion(String accion) {
        return "I".equals(accion) || "U".equals(accion);
    }
}
