package bo.bosque.com.impexpap.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Las reglas del legacy de cheques, tal como las aplicaba el JSF (decision 2 de CLAUDE.md: paridad).
 * Sin base ni Spring. Cada caso lleva el porque: varios fijan comportamientos que sorprenden y que
 * se conservan A PROPOSITO, para que nadie los "arregle" sin saberlo.
 */
class ReglasChequeTest {

    private static Date d(int anio, int mes, int dia) {
        return java.sql.Date.valueOf(LocalDate.of(anio, mes, dia));
    }

    // ----------------------------------------------------------------- Nro de cheque

    @Test
    @DisplayName("Nro de cheque: digitos y guion medio")
    void nroChequeValido() {
        assertNull(ReglasCheque.errorNroCheque("123-45"));
        assertNull(ReglasCheque.errorNroCheque("0012"));
    }

    @Test
    @DisplayName("Nro de cheque: letras y espacios se rechazan")
    void nroChequeInvalido() {
        assertNotNull(ReglasCheque.errorNroCheque("abc"));
        assertNotNull(ReglasCheque.errorNroCheque("12 3"));
        assertNotNull(ReglasCheque.errorNroCheque(null));
    }

    @Test
    @DisplayName("Nro de cheque: el {2,25} del regex del legacy NO limita el largo (se conserva)")
    void nroChequeSinLimiteDeLargo() {
        // El mensaje del JSF dice "entre 2 y 25", pero "[0-9?-]*{2,25}" en Java aplica {2,25} a una
        // expresion vacia. Lo que limita el largo es maxlength=45 del campo, no la regla.
        assertNull(ReglasCheque.errorNroCheque("1234567890123456789012345678901234567890"));
        assertNull(ReglasCheque.errorNroCheque("1"));
        assertNull(ReglasCheque.errorNroCheque(""), "vacio pasa la regla: lo frena required=true del campo");
        assertNull(ReglasCheque.errorNroCheque("12?3"), "el '?' esta dentro de la clase de caracteres");
    }

    // ----------------------------------------------------------------- A la orden de

    @Test
    @DisplayName("A la orden de: letras, acentos, enie, apostrofe y espacios")
    void aOrdenDe() {
        assertNull(ReglasCheque.errorAOrdenDe("José Ñandú O'Brien"));
        assertNull(ReglasCheque.errorAOrdenDe(""));
        assertNotNull(ReglasCheque.errorAOrdenDe("Juan123"));
        assertNotNull(ReglasCheque.errorAOrdenDe("S.R.L."));
        char[] largo = new char[101];
        java.util.Arrays.fill(largo, 'a');
        assertNotNull(ReglasCheque.errorAOrdenDe(new String(largo)));
    }

    // ----------------------------------------------------------------- Recibo y talonario

    @Test
    @DisplayName("Recibo manual: 1 a 25, letras, numeros, espacios y apostrofe")
    void reciboManual() {
        assertNull(ReglasCheque.errorReciboManual("0"));
        assertNull(ReglasCheque.errorReciboManual("AB 12"));
        assertNotNull(ReglasCheque.errorReciboManual(""));
        assertNotNull(ReglasCheque.errorReciboManual("a-b"));
    }

    @Test
    @DisplayName("Talonario manual: 1 a 25, letras, numeros y espacios (sin apostrofe)")
    void nroTalonario() {
        assertNull(ReglasCheque.errorNroTalonario("0"));
        assertNull(ReglasCheque.errorNroTalonario("T 100"));
        assertNotNull(ReglasCheque.errorNroTalonario("T'100"));
        assertNotNull(ReglasCheque.errorNroTalonario(""));
    }

    @Test
    @DisplayName("Cliente dejo el cheque (empleado 0): recibo debe ser 0")
    void clienteConRecibo() {
        assertTrue(ReglasCheque.erroresReciboTalonario(0, "0", "0").isEmpty());
        List<String> e = ReglasCheque.erroresReciboTalonario(0, "55", "1");
        assertEquals(1, e.size());
        assertTrue(e.get(0).contains("Cliente"));
    }

    @Test
    @DisplayName("Un empleado lo trajo: el recibo no puede ser 0")
    void empleadoSinRecibo() {
        List<String> e = ReglasCheque.erroresReciboTalonario(7, "0", "0");
        assertEquals(1, e.size());
        assertTrue(e.get(0).contains("empleado"));
    }

    @Test
    @DisplayName("Con recibo hay que indicar talonario")
    void reciboSinTalonario() {
        List<String> e = ReglasCheque.erroresReciboTalonario(7, "55", "0");
        assertEquals(1, e.size());
        assertTrue(e.get(0).contains("talonario"));
    }

    @Test
    @DisplayName("Los mensajes se acumulan, como en el legacy (no corta en el primero)")
    void erroresAcumulados() {
        // cliente + recibo != 0  => regla 1;  recibo != 0 + talonario 0 => regla 3
        assertEquals(2, ReglasCheque.erroresReciboTalonario(0, "55", "0").size());
    }

    @Test
    @DisplayName("El par talonario/recibo solo se consulta a la base si ninguno es 0")
    void validarTalonarioSoloSiNingunoEsCero() {
        assertFalse(ReglasCheque.requiereValidarTalonario("0", "T1"));
        assertFalse(ReglasCheque.requiereValidarTalonario("55", "0"));
        assertFalse(ReglasCheque.requiereValidarTalonario("0", "0"));
        assertTrue(ReglasCheque.requiereValidarTalonario("55", "T1"));
    }

    @Test
    @DisplayName("El cero se compara exacto: ' 0' o '00' NO son el cero del legacy")
    void ceroExacto() {
        // Como "0".equals(x): un recibo "00" cuenta como recibo cargado, asi que con el cliente como
        // quien lo dejo salen la regla 1 (cliente con recibo) y la 3 (recibo sin talonario).
        assertEquals(2, ReglasCheque.erroresReciboTalonario(0, "00", "0").size());
        assertTrue(ReglasCheque.requiereValidarTalonario("00", "T1"));
        assertTrue(ReglasCheque.requiereValidarTalonario(" 0", "T1"));
    }

    // ----------------------------------------------------------------- Nombre del banco

    @Test
    @DisplayName("Nombre del banco: el validador del legacy NO admite el guion, el punto ni los acentos (se conserva)")
    void nombreBanco() {
        // "[a-zA-z0-9 ' ' '-'  /S]": el '-' entre dos apostrofes es el RANGO ' a ', no un guion literal.
        assertNull(ReglasCheque.errorNombreBanco("Union BU"));
        assertNull(ReglasCheque.errorNombreBanco("Mercantil Santa Cruz  MSC"));
        assertNull(ReglasCheque.errorNombreBanco("BISA"));
        assertNull(ReglasCheque.errorNombreBanco("Banco FIE"));
        assertNull(ReglasCheque.errorNombreBanco("Caja Los Andes S/A"));
        assertNull(ReglasCheque.errorNombreBanco("O'Higgins"), "el apostrofe si entra");
        assertNotNull(ReglasCheque.errorNombreBanco("BANCO-X"), "el guion NO entra");
        assertNotNull(ReglasCheque.errorNombreBanco("Banco Union S.A."), "el punto NO entra");
        assertNotNull(ReglasCheque.errorNombreBanco("Económico"), "los acentos NO entran");
        assertNotNull(ReglasCheque.errorNombreBanco("Mercury (cuenta propia)"), "los parentesis NO entran");
        assertNotNull(ReglasCheque.errorNombreBanco("AB"), "menos de 3");
        assertNotNull(ReglasCheque.errorNombreBanco(null));
    }

    // ----------------------------------------------------------------- Observacion y Nro SAP

    @Test
    @DisplayName("Observacion: vacia es valida (el JSF no valida campos vacios)")
    void observacionVacia() {
        assertNull(ReglasCheque.errorObservacion(null));
        assertNull(ReglasCheque.errorObservacion(""));
    }

    @Test
    @DisplayName("Observacion: 2 a 200, sin guion ni barra")
    void observacion() {
        assertNull(ReglasCheque.errorObservacion("Entregado en Para Cobranza"));
        assertNull(ReglasCheque.errorObservacion("ok, ok; ok: ok."));
        assertNotNull(ReglasCheque.errorObservacion("a"));
        assertNotNull(ReglasCheque.errorObservacion("con - guion"));
        assertNotNull(ReglasCheque.errorObservacion("12/10/2026"));
    }

    @Test
    @DisplayName("Nro SAP: 2 a 40, sin guion")
    void nroSap() {
        assertNull(ReglasCheque.errorNroSap("12345"));
        assertNotNull(ReglasCheque.errorNroSap("1"));
        assertNotNull(ReglasCheque.errorNroSap("123-45"));
        assertNotNull(ReglasCheque.errorNroSap(null));
    }

    // ----------------------------------------------------------------- ceros a la izquierda

    @Test
    @DisplayName("Quita los ceros a la izquierda del Nro de cheque")
    void ceros() {
        assertEquals("123", ReglasCheque.sinCerosIzquierda("00123"));
        assertEquals("10", ReglasCheque.sinCerosIzquierda("10"));
        assertEquals("", ReglasCheque.sinCerosIzquierda("0"), "solo ceros queda vacio (el legacy revienta)");
        assertEquals("", ReglasCheque.sinCerosIzquierda("000"));
        assertNull(ReglasCheque.sinCerosIzquierda(null));
    }

    // ----------------------------------------------------------------- Fecha de cobro

    @Test
    @DisplayName("Fecha de cobro: +-28 dias de la fecha del cheque, extremos incluidos")
    void cobroEnRango() {
        Date cheque = d(2026, 10, 1);
        assertTrue(ReglasCheque.cobroEnRango(d(2026, 10, 1), cheque));
        assertTrue(ReglasCheque.cobroEnRango(d(2026, 10, 29), cheque), "+28 incluido");
        assertFalse(ReglasCheque.cobroEnRango(d(2026, 10, 30), cheque), "+29 fuera");
        assertTrue(ReglasCheque.cobroEnRango(d(2026, 9, 3), cheque), "-28 incluido");
        assertFalse(ReglasCheque.cobroEnRango(d(2026, 9, 2), cheque), "-29 fuera");
    }

    @Test
    @DisplayName("Fecha de cobro: sin fechas no hay rango")
    void cobroSinFechas() {
        assertFalse(ReglasCheque.cobroEnRango(null, d(2026, 10, 1)));
        assertFalse(ReglasCheque.cobroEnRango(d(2026, 10, 1), null));
    }

    @Test
    @DisplayName("La fecha de una accion no puede ser anterior a hoy")
    void noAnteriorAHoy() {
        LocalDate hoy = LocalDate.of(2026, 10, 2);
        assertTrue(ReglasCheque.noAnteriorA(d(2026, 10, 2), hoy));
        assertTrue(ReglasCheque.noAnteriorA(d(2026, 10, 3), hoy));
        assertFalse(ReglasCheque.noAnteriorA(d(2026, 10, 1), hoy));
        assertFalse(ReglasCheque.noAnteriorA(null, hoy));
    }

    // ----------------------------------------------------------------- Acciones

    @Test
    @DisplayName("REC, CUS y TRASP no se guardan desde el formulario de acciones")
    void estadosReservados() {
        assertTrue(ReglasCheque.esEstadoReservado("REC"));
        assertTrue(ReglasCheque.esEstadoReservado("CUS"));
        assertTrue(ReglasCheque.esEstadoReservado("TRASP"));
        assertFalse(ReglasCheque.esEstadoReservado("DEV"));
        assertFalse(ReglasCheque.esEstadoReservado("COB"));
        assertFalse(ReglasCheque.esEstadoReservado(null));
    }

    @Test
    @DisplayName("La observacion de una nueva fecha de cobro lleva el prefijo del legacy")
    void observacionFechaCobro() {
        assertEquals("Nueva Fecha de Cobro = 05/11/2026 . por pedido",
                ReglasCheque.observacionFechaCobro(LocalDate.of(2026, 11, 5), "por pedido"));
        assertEquals("Nueva Fecha de Cobro = 05/11/2026 . ",
                ReglasCheque.observacionFechaCobro(LocalDate.of(2026, 11, 5), null));
    }
}
