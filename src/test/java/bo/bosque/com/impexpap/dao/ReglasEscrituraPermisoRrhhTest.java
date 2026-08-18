package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.config.SpConfirmableException;
import bo.bosque.com.impexpap.utils.SpEscritura;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Las reglas que separan un abono legítimo de un error caro. <b>Ninguna la valida el SP</b>: los
 * tres {@code p_abm_} de este módulo aceptan días negativos, motivos de 400 caracteres y fechas de
 * 2099 sin chistar, así que si estas comprobaciones se caen no se cae un test — se paga de más.
 *
 * <p>Es un test puro, sin Spring y sin base: por eso las reglas son estáticas y
 * package-private en cada DAO.
 */
class ReglasEscrituraPermisoRrhhTest {

    // ── días: los dos umbrales, que NO son el mismo ──────────────────────

    @Test
    @DisplayName("Vacación asignada: cero días es válido — es 'este año no le corresponde nada'")
    void vacacionAsignadaAceptaCero() {
        VacacionAsignadaDao.validarDias(0, false);      // 446 de 1.424 filas reales están así
        VacacionAsignadaDao.validarDias(15, false);
        VacacionAsignadaDao.validarDias(30, false);
    }

    @Test
    @DisplayName("Abono de días: cero NO es válido, ahí el umbral es estricto")
    void abonoRechazaCero() {
        assertThrows(SpBusinessException.class, () -> AbonoDiasDao.validarDias(0, false));
        assertThrows(SpBusinessException.class, () -> AbonoDiasDao.validarDias(-1, false));
        AbonoDiasDao.validarDias(0.5, false);           // el mínimo real medido en la tabla
    }

    @Test
    @DisplayName("Días negativos: nunca, ni confirmados")
    void nadaDeNegativos() {
        assertThrows(SpBusinessException.class, () -> VacacionAsignadaDao.validarDias(-0.5, true));
        assertThrows(SpBusinessException.class, () -> AbonoDiasDao.validarDias(-0.5, true));
    }

    @Test
    @DisplayName("Fuera del rango histórico se pide confirmación; pasado el tope no pasa ni confirmado")
    void topesDeCordura() {
        assertThrows(SpBusinessException.class, () -> VacacionAsignadaDao.validarDias(31, false));
        VacacionAsignadaDao.validarDias(31, true);
        assertThrows(SpBusinessException.class, () -> VacacionAsignadaDao.validarDias(61, true));

        assertThrows(SpBusinessException.class, () -> AbonoDiasDao.validarDias(23, false));
        AbonoDiasDao.validarDias(23, true);
        assertThrows(SpBusinessException.class, () -> AbonoDiasDao.validarDias(61, true));
    }

    // ── motivo: obligatorio, y con el largo REAL de cada columna ─────────

    @Test
    @DisplayName("Motivo obligatorio en los dos, con o sin espacios")
    void motivoObligatorio() {
        assertThrows(SpBusinessException.class, () -> VacacionAsignadaDao.validarMotivo(null));
        assertThrows(SpBusinessException.class, () -> VacacionAsignadaDao.validarMotivo("   "));
        assertThrows(SpBusinessException.class, () -> AbonoDiasDao.validarMotivo(null));
        assertThrows(SpBusinessException.class, () -> AbonoDiasDao.validarMotivo(" a "));
        assertEquals("Gestión 2026", AbonoDiasDao.validarMotivo("  Gestión 2026  "));
    }

    @Test
    @DisplayName("El motivo se corta en el largo de la columna y se avisa, nunca se trunca callado")
    void motivoHastaElLargoDeLaColumna() {
        VacacionAsignadaDao.validarMotivo(repetir(300));
        assertThrows(SpBusinessException.class, () -> VacacionAsignadaDao.validarMotivo(repetir(301)));

        AbonoDiasDao.validarMotivo(repetir(50));
        // El caso real que hoy rompe el SP con "String or binary data would be truncated"
        String largo = "Compensación por el inventario del 21 de septiembre";
        assertEquals(51, largo.length(), "el ejemplo tiene que pasarse de 50 o no prueba nada");
        assertThrows(SpBusinessException.class, () -> AbonoDiasDao.validarMotivo(largo));
    }

    @Test
    @DisplayName("La ñ y los acentos pasan: el regex del legacy los rechazaba y era un defecto")
    void motivoConEnie() {
        assertEquals("Compensación día del niño",
                AbonoDiasDao.validarMotivo("Compensación día del niño"));
    }

    // ── fecha ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("La fecha del abono es obligatoria y no puede irse más de un año al futuro")
    void fechaDelAbono() {
        assertThrows(SpBusinessException.class, () -> AbonoDiasDao.validarFecha(null));
        AbonoDiasDao.validarFecha(new Date());
        AbonoDiasDao.validarFecha(corrido(Calendar.YEAR, -5));   // las regularizaciones son el caso normal
        assertThrows(SpBusinessException.class,
                () -> AbonoDiasDao.validarFecha(corrido(Calendar.YEAR, 2)));
    }

    // ── auditoría y comparación de fechas ────────────────────────────────

    @Test
    @DisplayName("Sin usuario auditor no se escribe: la columna es NOT NULL")
    void exigeUsuarioAuditor() {
        assertThrows(SpBusinessException.class, () -> SpEscritura.exigirUsuarioAuditor(0));
        SpEscritura.exigirUsuarioAuditor(7);
    }

    @Test
    @DisplayName("El mismo día es el mismo día aunque cambie la hora")
    void mismoDiaIgnoraLaHora() {
        Calendar c = Calendar.getInstance();
        c.set(2024, Calendar.MARCH, 5, 8, 30, 0);
        Date manana = c.getTime();
        c.set(Calendar.HOUR_OF_DAY, 20);
        Date noche = c.getTime();
        c.set(Calendar.DAY_OF_MONTH, 6);
        Date otroDia = c.getTime();

        assertTrue(SpEscritura.mismoDia(manana, noche));
        // El caso de todos los días: la fecha del formulario contra la DATE que devuelve el SP.
        assertTrue(SpEscritura.mismoDia(manana, new java.sql.Date(manana.getTime())));
        assertFalse(SpEscritura.mismoDia(manana, otroDia));
        assertFalse(SpEscritura.mismoDia(manana, null));
    }

    // ── vacación colectiva: el motivo va a trh_permiso, que es varchar(100) ──

    @Test
    @DisplayName("Vacación colectiva: el motivo se corta en 100, el largo de trh_permiso.motivo")
    void motivoDeLaVacacionColectiva() {
        assertThrows(SpBusinessException.class, () -> PermisoDao.validarMotivo(null));
        assertThrows(SpBusinessException.class, () -> PermisoDao.validarMotivo(" a "));
        assertEquals("Cierre de planta", PermisoDao.validarMotivo("  Cierre de planta  "));
        PermisoDao.validarMotivo(repetir(100));
        // Uno más y el INSERT muere con "String or binary data would be truncated" — y en un lote
        // de 85 filas eso significa que no entra ninguna.
        assertThrows(SpBusinessException.class, () -> PermisoDao.validarMotivo(repetir(101)));
    }

    // ── vacación PAGADA: el único número que nadie calcula por el usuario ──

    @Test
    @DisplayName("Días a pagar: medio día es el piso y van de medio en medio")
    void diasPagadosPiso() {
        PermisoDao.validarDiasPagados(0.5, false);
        PermisoDao.validarDiasPagados(15, false);
        assertThrows(SpBusinessException.class, () -> PermisoDao.validarDiasPagados(0, false));
        assertThrows(SpBusinessException.class, () -> PermisoDao.validarDiasPagados(-1, false));
        // El input del legacy es step=0.5; un 0,3 es un tipeo, no un pago.
        assertThrows(SpBusinessException.class, () -> PermisoDao.validarDiasPagados(0.75, false));
    }

    @Test
    @DisplayName("Días a pagar: 30 se confirma, 247 —la fila real de 2020— no pasa ni confirmada")
    void diasPagadosTope() {
        assertThrows(SpConfirmableException.class, () -> PermisoDao.validarDiasPagados(31, false));
        PermisoDao.validarDiasPagados(31, true);
        assertThrows(SpBusinessException.class, () -> PermisoDao.validarDiasPagados(247, true));
    }

    // ── "horas a reponer": el cartel del modal, que no se guarda en ningún lado ──

    @Test
    @DisplayName("Horas a reponer: sólo 'otro' y 'pcr', y son las horas del permiso")
    void horasAReponerSoloDosTipos() {
        assertEquals(8.0,  PermisoDao.horasAReponer("otro", 1.0), 1e-9);
        assertEquals(4.0,  PermisoDao.horasAReponer("PCR", 0.5), 1e-9);   // el combo manda minúsculas
        assertEquals(0.0,  PermisoDao.horasAReponer("vac", 5.0), 1e-9);
        assertEquals(0.0,  PermisoDao.horasAReponer("baja", 3.0), 1e-9);
        assertEquals(0.0,  PermisoDao.horasAReponer(null, 3.0), 1e-9);
    }

    // ── auxiliares ───────────────────────────────────────────────────────

    private static String repetir(int largo) {
        StringBuilder sb = new StringBuilder(largo);
        for (int i = 0; i < largo; i++) sb.append('x');
        return sb.toString();
    }

    private static Date corrido(int campo, int cuanto) {
        Calendar c = Calendar.getInstance();
        c.add(campo, cuanto);
        return c.getTime();
    }
}
