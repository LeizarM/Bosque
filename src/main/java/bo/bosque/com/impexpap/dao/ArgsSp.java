package bo.bosque.com.impexpap.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Map;

/**
 * Utilidades para armar el {@code Map} de parametros de un SP y leer columnas por posicion.
 *
 * <p>Los {@code p_list_} de cheques usan {@code @x IS NULL} como "sin filtro": un parametro que no se
 * manda queda en su {@code DEFAULT NULL}, que es lo mismo que mandarlo en null, y un {@code 0} o un
 * texto vacio serian un filtro real (por eso {@link #poner} omite los null y {@link #texto} convierte
 * el vacio en null).
 */
final class ArgsSp {

    private ArgsSp() {
    }

    /** Agrega el parametro solo si trae valor. */
    static void poner(Map<String, Object> p, String nombre, Object valor) {
        if (valor != null) p.put(nombre, valor);
    }

    /** Texto recortado; vacio cuenta como null (el SP lo toma como "sin filtro"). */
    static String texto(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /** Un id positivo, o null si es null o 0 (en estos SP el 0 no es un filtro: el legacy lo manda como NULL). */
    static Integer idONulo(Integer v) {
        return v == null || v == 0 ? null : v;
    }

    // ---- lectura por posicion (null-safe)

    static Integer entero(ResultSet rs, int col) throws SQLException {
        int v = rs.getInt(col);
        return rs.wasNull() ? null : v;
    }

    static Long largo(ResultSet rs, int col) throws SQLException {
        long v = rs.getLong(col);
        return rs.wasNull() ? null : v;
    }

    static Double doble(ResultSet rs, int col) throws SQLException {
        double v = rs.getDouble(col);
        return rs.wasNull() ? null : v;
    }

    /**
     * Un {@code date}. jTDS lo entrega como texto ("2026-08-20") y {@code getDate} lo convierte
     * bien; {@code getTimestamp} tambien, pero no para {@code datetimeoffset}.
     */
    static Date fecha(ResultSet rs, int col) throws SQLException {
        return rs.getDate(col);
    }
}
