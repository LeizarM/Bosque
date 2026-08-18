package bo.bosque.com.impexpap.dao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * <b>La fecha que devuelve el driver, venga como venga.</b>
 *
 * <h3>El 500 que este test tapa</h3>
 * En producción, {@code /permiso-rrhh/saldo/ficha} tiraba
 * {@code ClassCastException: java.lang.String cannot be cast to java.util.Date}
 * en {@code completarAlcanceD0}, una y otra vez. La línea casteaba directo lo
 * que salía de {@code queryForList}, y con eso se caía <b>toda</b> la ficha: no
 * un rótulo, el endpoint entero — y con él la pestaña «Saldo» y el botón de
 * abonar días, que necesita la relación laboral vigente que ese endpoint trae.
 *
 * <p>El motivo de fondo es que {@code tb_relEmplEmpr.fechaIni} es de tipo
 * {@code date} —el resto del esquema usa {@code datetime}— y ese tipo es
 * justamente donde los drivers de SQL Server difieren: unos devuelven
 * {@code java.sql.Date}, otros el texto crudo. Este código no decide cuál toca,
 * así que no puede darlo por sentado.
 *
 * <p>Ningún test lo vio porque los del DAO no tocan la base y los del
 * controlador mockean el DAO: el cast vivía en la única franja que nadie
 * cubría. Éste es puro {@code aFecha}, sin base ni Spring, que es lo que se
 * puede probar y lo que se rompió.
 */
class FechaDelDriverTest {

    /** 2022-08-01 a medianoche, que es lo que representa un {@code date}. */
    private static Date primeroDeAgosto2022() {
        Calendar c = new GregorianCalendar(2022, Calendar.AUGUST, 1, 0, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    @Test
    @DisplayName("un java.sql.Date pasa tal cual: es la ruta normal")
    void sqlDate() {
        java.sql.Date entrada = new java.sql.Date(primeroDeAgosto2022().getTime());
        assertEquals(entrada, PermisoDao.aFecha(entrada));
    }

    @Test
    @DisplayName("un Timestamp también: sigue siendo un Date")
    void timestamp() {
        java.sql.Timestamp entrada =
                new java.sql.Timestamp(primeroDeAgosto2022().getTime());
        assertEquals(entrada, PermisoDao.aFecha(entrada));
    }

    @Test
    @DisplayName("el texto 'yyyy-MM-dd' se interpreta — el caso que tiraba el 500")
    void textoSoloFecha() {
        assertEquals(primeroDeAgosto2022(), PermisoDao.aFecha("2022-08-01"));
    }

    @Test
    @DisplayName("el texto con hora también: se queda con el día")
    void textoConHora() {
        assertEquals(
                primeroDeAgosto2022(),
                PermisoDao.aFecha("2022-08-01 00:00:00.0"));
    }

    @Test
    @DisplayName("lo que no es una fecha devuelve null, NO revienta")
    void basura() {
        // El rótulo de alcance sabe vivir sin la fecha. Una ficha sin rótulo es
        // infinitamente mejor que ninguna ficha, que es lo que pasaba antes.
        assertNull(PermisoDao.aFecha(null));
        assertNull(PermisoDao.aFecha(""));
        assertNull(PermisoDao.aFecha("no soy una fecha"));
        assertNull(PermisoDao.aFecha("2022-13-45"));
        assertNull(PermisoDao.aFecha(12345));
    }
}
