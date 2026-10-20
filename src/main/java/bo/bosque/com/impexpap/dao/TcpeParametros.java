package bo.bosque.com.impexpap.dao;

import java.time.LocalDate;
import java.util.Map;

/**
 * {@code Map} de parámetros de los SP {@code tcpe_}. En los {@code p_list} NULL es "sin filtro": lo nulo no se manda
 * y un código en 0 cuenta como nulo (ninguna PK vale 0 y filtraría cero filas). jTDS no enlaza
 * {@code java.time.LocalDate}, así que se pasa a {@code java.sql.Date}.
 */
final class TcpeParametros {

    private TcpeParametros() {
    }

    /** Agrega el parámetro solo si trae valor, ya convertido para jTDS. */
    static void poner(Map<String, Object> p, String nombre, Object valor) {
        Object v = paraSp(valor);
        if (v != null) p.put(nombre, v);
    }

    /** Agrega un filtro: además de los nulos descarta los códigos en 0 y el texto vacío. */
    static void filtro(Map<String, Object> p, String nombre, Object valor) {
        Object v = paraFiltro(valor);
        if (v != null) p.put(nombre, v);
    }

    /** Un código positivo, o null. */
    static Long id(Long v) {
        return v == null || v <= 0 ? null : v;
    }

    static Object paraSp(Object valor) {
        if (valor instanceof LocalDate) return java.sql.Date.valueOf((LocalDate) valor);
        return valor;
    }

    static Object paraFiltro(Object valor) {
        if (valor instanceof Number && ((Number) valor).doubleValue() == 0) return null;
        if (valor instanceof String) {
            String t = ((String) valor).trim();
            return t.isEmpty() ? null : t;
        }
        return paraSp(valor);
    }
}
