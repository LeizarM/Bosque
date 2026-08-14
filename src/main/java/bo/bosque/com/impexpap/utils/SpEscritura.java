package bo.bosque.com.impexpap.utils;

import bo.bosque.com.impexpap.config.SpBusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;

import java.util.Date;

/**
 * Cómo se ejecuta una escritura contra un SP <b>viejo</b>: los que no declaran
 * {@code @error/@errormsg/@idGenerado} y por lo tanto no se pueden llamar con
 * {@link SpHelper#ejecutarAbmMap} (les agregaría tres parámetros OUTPUT que no existen y el
 * {@code EXEC} fallaría).
 *
 * <h3>Por qué no devuelve {@code boolean} como los DAO viejos</h3>
 * El patrón {@code catch (DataAccessException) { return false; }} que usan {@code ColorDao} y
 * compañía es incompatible con {@code @Transactional}: Spring sólo hace rollback si sale una
 * excepción de Java, así que tragarla dejaría media carga colectiva escrita y el método diría que
 * todo salió bien. Acá se lanza {@link SpBusinessException}, que
 * {@code GlobalExceptionHandler} convierte en un 400 con el motivo.
 *
 * <h3>Y un rowcount de cero tampoco es benigno — salvo en el UPDATE</h3>
 * Estos SP son una sucesión de {@code IF(@ACCION='I') ... IF(@ACCION='U') ...} sin {@code ELSE}:
 * una ACCION mal escrita no hace nada y no falla. En un DELETE, cero filas significa que el id no
 * existe. Eso se reporta como error, nunca como éxito silencioso — {@link #ejecutar}.
 *
 * <p><b>El UPDATE es la excepción y va por {@link #ejecutarUpdate}.</b> Ver el javadoc de ese
 * método: ahí el cero no significa nada.
 */
public final class SpEscritura {

    private static final Logger log = LoggerFactory.getLogger(SpEscritura.class);

    private SpEscritura() { }

    /**
     * @param queHacia se completa como "No se pudo &lt;queHacia&gt;: …", así que va en infinitivo
     *                 ("dar de alta el abono de días")
     * @return las filas que informó el driver — sólo sirve para "algo pasó", porque los triggers
     *         {@code dai_/dau_/dad_} emiten conteos propios que se mezclan con éste
     */
    public static int ejecutar(JdbcTemplate jdbcTemplate, String sql, String queHacia,
                               PreparedStatementSetter setter) {
        int filas = ejecutarUpdate(jdbcTemplate, sql, queHacia, setter);
        if (filas == 0) {
            throw new SpBusinessException(
                    "No se pudo " + queHacia + ": la base no registró el cambio.");
        }
        return filas;
    }

    /**
     * Igual que {@link #ejecutar}, pero <b>sin tratar el rowcount 0 como error</b>. Es la que usan
     * las EDICIONES.
     *
     * <h3>Por qué acá el cero no significa nada</h3>
     * {@code trh_vacacionAsignada} y {@code trh_abonoDias} tienen un trigger <b>INSTEAD OF
     * UPDATE</b> ({@code dau_vacacionAsignada}, {@code dau_abonoDias}) y los dos abren con
     * {@code SET NOCOUNT ON}. El UPDATE real lo hace el trigger, con el conteo apagado: el driver
     * puede recibir 0 <b>habiendo guardado perfectamente</b>. Con la comprobación puesta, toda
     * edición correcta respondía "la base no registró el cambio" y el usuario volvía a apretar
     * sobre una fila que ya estaba bien.
     *
     * <p>Y la verificación que sí sirve ya existe y es mejor: los DAO <b>releen la fila</b> después
     * del UPDATE y devuelven lo que quedó guardado. Un rowcount de un trigger no prueba nada; la
     * fila releída, sí.
     *
     * <p>El DELETE y el INSERT siguen yendo por {@link #ejecutar}: ahí no hay INSTEAD OF, el
     * conteo llega de verdad, y un 0 significa que el id no existía.
     */
    public static int ejecutarUpdate(JdbcTemplate jdbcTemplate, String sql, String queHacia,
                                     PreparedStatementSetter setter) {
        try {
            return jdbcTemplate.update(sql, setter);
        } catch (DataAccessException ex) {
            log.error("Error de base al {}", queHacia, ex);
            throw new SpBusinessException("No se pudo " + queHacia + ": " + causaLegible(ex));
        }
    }

    /** El mensaje del motor, no el envoltorio de Spring: es el que dice "truncamiento" o "FK". */
    public static String causaLegible(DataAccessException ex) {
        Throwable raiz = ex.getMostSpecificCause();
        String msg = raiz != null ? raiz.getMessage() : ex.getMessage();
        return msg == null ? "error desconocido en la base de datos." : msg.trim();
    }

    /**
     * {@code audUsuarioI} es NOT NULL en las dos tablas y sale del token, nunca del body. Sin
     * este control el INSERT muere con un error de SQL crudo que en pantalla se lee como
     * "se rompió el sistema".
     */
    public static void exigirUsuarioAuditor(long audUsuarioI) {
        if (audUsuarioI <= 0) {
            throw new SpBusinessException(
                    "No se pudo determinar el usuario que registra la operación.");
        }
    }

    /**
     * El mismo día, comparando por fecha y no por instante.
     *
     * <p>Vía {@code java.sql.Date#toString()}, que rinde {@code yyyy-MM-dd} en la zona local.
     * {@code toInstant()} no sirve: en {@code java.sql.Date} lanza
     * {@code UnsupportedOperationException}, y esas fechas son justamente las que devuelven estos
     * SP.
     */
    public static boolean mismoDia(Date a, Date b) {
        if (a == null || b == null) return false;
        return new java.sql.Date(a.getTime()).toString()
                .equals(new java.sql.Date(b.getTime()).toString());
    }
}
