package bo.bosque.com.impexpap.dao;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Lectura de las filas de un PIVOT de precios, que llegan como mapa.
 *
 * <p>Las ramas que pivotean por lista de precio ({@code p_list_ArticuloProp} F y H,
 * {@code p_list_producto} I y K) devuelven doce columnas llamadas {@code "1".."12"}.
 * No se renombran en el proc porque las leen por nombre los reportes Jasper del JSF,
 * que sigue en produccion sobre las mismas tablas; y un nombre numerico no es una
 * propiedad Java, asi que {@code BeanPropertyRowMapper} no las puede mapear. Por eso
 * esas ramas se leen con {@code SpHelper.ejecutarListadoDinamico} y el DAO arma el DTO
 * con esta clase.
 *
 * <p>El mapa que entrega {@code JdbcTemplate.queryForList} no distingue mayusculas en
 * las claves, pero los tipos dependen del driver: un {@code float} llega como
 * {@code Double}, un {@code decimal} como {@code BigDecimal}. Aca se convierte en vez
 * de castear.
 */
final class FilaPivote {

    /** Cantidad de listas de precio que pivotean los procs. */
    static final int LISTAS = 12;

    private FilaPivote() {
    }

    static BigDecimal decimal(Map<String, Object> fila, String clave) {
        Object v = fila.get(clave);
        if (v == null) {
            return null;
        }
        if (v instanceof BigDecimal) {
            return (BigDecimal) v;
        }
        if (v instanceof Number) {
            // toString y no doubleValue: new BigDecimal(0.1d) arrastra la cola binaria.
            return new BigDecimal(v.toString());
        }
        String texto = v.toString().trim();
        return texto.isEmpty() ? null : new BigDecimal(texto);
    }

    static Integer entero(Map<String, Object> fila, String clave) {
        Object v = fila.get(clave);
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        String texto = v.toString().trim();
        return texto.isEmpty() ? null : Integer.valueOf(texto);
    }

    static Long largo(Map<String, Object> fila, String clave) {
        Object v = fila.get(clave);
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        String texto = v.toString().trim();
        return texto.isEmpty() ? null : Long.valueOf(texto);
    }

    static String texto(Map<String, Object> fila, String clave) {
        Object v = fila.get(clave);
        return v == null ? null : v.toString();
    }

    /**
     * El precio (o el valor) de la lista {@code n}, columna {@code "n"} del PIVOT.
     *
     * <p>Si la columna no vino, se prueba con {@code valorN} y {@code precioN}: es como
     * las devolvia una version anterior de tpr_ArticuloPropuesto.sql, que se llego a
     * ejecutar en BOSQUE2PRUEBA y renombraba las ramas F y H. Sin esto, una base que
     * todavia tiene esa version imprime las doce listas vacias.
     */
    static BigDecimal lista(Map<String, Object> fila, int n) {
        for (String clave : new String[]{String.valueOf(n), "valor" + n, "precio" + n}) {
            if (fila.containsKey(clave)) {
                return decimal(fila, clave);
            }
        }
        return null;
    }
}
