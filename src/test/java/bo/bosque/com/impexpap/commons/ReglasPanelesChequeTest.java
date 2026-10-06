package bo.bosque.com.impexpap.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Las reglas de los tres paneles del detalle del cheque, tal como las aplicaba el JSF (decision 2 de CLAUDE.md: paridad).
 * Sin base ni Spring. Varios casos fijan comportamientos que sorprenden y que se conservan A PROPOSITO.
 */
class ReglasPanelesChequeTest {

    private static Date d(int anio, int mes, int dia) {
        return java.sql.Date.valueOf(LocalDate.of(anio, mes, dia));
    }

    private static String repetir(char c, int n) {
        char[] a = new char[n];
        Arrays.fill(a, c);
        return new String(a);
    }

    // ----------------------------------------------------------------- nota de remision: el regex literal

    @Test
    @DisplayName("el regex de la nota es el del legacy, literal: '^[0-9  ]{5,10}' con DOS espacios normales (0x20) dentro de la clase")
    void regexLiteral() {
        assertEquals("^[0-9  ]{5,10}", ReglasPanelesCheque.NOTA_REMISION.pattern());
        // NotaRemisionManagedBean.java:167: cadenaRegex = "^[0-9  ]{5,10}"; los dos espacios son 0x20, ninguno es un espacio duro
        String p = ReglasPanelesCheque.NOTA_REMISION.pattern();
        assertEquals(' ', p.charAt(5));
        assertEquals(' ', p.charAt(6));
        assertEquals(14, p.length());
    }

    @Test
    @DisplayName("nota: digitos y espacios, de 5 a 10 caracteres")
    void notaValida() {
        assertNull(ReglasPanelesCheque.errorNotaRemision("12345"));
        assertNull(ReglasPanelesCheque.errorNotaRemision("1234567890"));
        assertNull(ReglasPanelesCheque.errorNotaRemision("262211881"));
        assertNull(ReglasPanelesCheque.errorNotaRemision(" 17330357"), "hay notas reales con un espacio al inicio");
        assertNull(ReglasPanelesCheque.errorNotaRemision("4386 4420"), "y con un espacio en el medio");
        assertNull(ReglasPanelesCheque.errorNotaRemision("00000"), "los ceros valen");
        assertNull(ReglasPanelesCheque.errorNotaRemision("     "), "5 espacios cumplen el regex (se conserva)");
    }

    @Test
    @DisplayName("nota: menos de 5, mas de 10, letras o simbolos se rechazan nombrando el valor y lo que sobra")
    void notaInvalida() {
        String corta = ReglasPanelesCheque.errorNotaRemision("1234");
        assertEquals("El número de la nota de remisión «1234» no es válido: es demasiado corto (4 caracteres). "
                + "Solo acepta números y espacios, de 5 a 10 caracteres.", corta);

        String larga = ReglasPanelesCheque.errorNotaRemision("12345678901");
        assertEquals("El número de la nota de remisión «12345678901» no es válido: es demasiado largo (11 caracteres). "
                + "Solo acepta números y espacios, de 5 a 10 caracteres.", larga);

        assertEquals("El número de la nota de remisión «12a45-9» no es válido: tiene caracteres que no se aceptan («a», «-»). "
                + "Solo acepta números y espacios, de 5 a 10 caracteres.", ReglasPanelesCheque.errorNotaRemision("12a45-9"));
    }

    @Test
    @DisplayName("nota: vacia o null es 'falta' (el campo es required)")
    void notaFalta() {
        assertEquals(MensajesCheque.FALTA_NOTA_REMISION, ReglasPanelesCheque.errorNotaRemision(null));
        assertEquals(MensajesCheque.FALTA_NOTA_REMISION, ReglasPanelesCheque.errorNotaRemision(""));
    }

    @Test
    @DisplayName("el 'un caracter' del mensaje coincide con el regex: cada caracter se acepta si y solo si el patron completo lo acepta")
    void paridadCaracterPorCaracter() {
        for (int i = 0; i < 0x250; i++) {
            char c = (char) i;
            boolean porCaracter = ReglasPanelesCheque.NOTA_REMISION_CAR.matcher(String.valueOf(c)).matches();
            boolean porPatron = ReglasPanelesCheque.NOTA_REMISION.matcher(repetir(c, 5)).matches();
            assertEquals(porPatron, porCaracter, "caracter U+" + Integer.toHexString(i));
        }
    }

    // ----------------------------------------------------------------- factura

    @Test
    @DisplayName("factura: entero de 1 a 999999 (el maxlength=6 del campo); mayor que cero es WizardCheque.java:1043")
    void factura() {
        assertNull(ReglasPanelesCheque.errorNroFactura(1));
        assertNull(ReglasPanelesCheque.errorNroFactura(1856));
        assertNull(ReglasPanelesCheque.errorNroFactura(999_999));

        assertEquals(MensajesCheque.FALTA_NRO_FACTURA, ReglasPanelesCheque.errorNroFactura(null));
        assertEquals("El número de factura debe ser mayor a cero y escribiste 0. Escribe el número real de la factura de SAP.",
                ReglasPanelesCheque.errorNroFactura(0));
        assertEquals("El número de factura debe ser mayor a cero y escribiste -1. Escribe el número real de la factura de SAP.",
                ReglasPanelesCheque.errorNroFactura(-1));
        assertEquals("El número de factura «1000000» es demasiado largo: tiene 7 dígitos y el campo acepta hasta 6 "
                + "(como máximo 999999). Revisa el número de la factura de SAP.", ReglasPanelesCheque.errorNroFactura(1_000_000));
    }

    @Test
    @DisplayName("fecha de la factura: obligatoria; cualquier fecha vale (sin rango)")
    void fechaFactura() {
        assertEquals(MensajesCheque.FALTA_FECHA_FACTURA, ReglasPanelesCheque.errorFechaFactura(null));
        assertNull(ReglasPanelesCheque.errorFechaFactura(d(2026, 8, 31)));
        assertNull(ReglasPanelesCheque.errorFechaFactura(d(1999, 1, 1)));
        assertNull(ReglasPanelesCheque.errorFechaFactura(d(2099, 12, 31)));
    }

    @Test
    @DisplayName("los errores de una nota se acumulan en el orden del formulario: nota, factura, fecha")
    void erroresDeLaNota() {
        List<String> todos = ReglasPanelesCheque.erroresNotaRemision("12a", 0, null);
        assertEquals(3, todos.size());
        assertTrue(todos.get(0).startsWith("El número de la nota de remisión «12a»"));
        assertTrue(todos.get(1).startsWith("El número de factura debe ser mayor a cero"));
        assertEquals(MensajesCheque.FALTA_FECHA_FACTURA, todos.get(2));

        assertTrue(ReglasPanelesCheque.erroresNotaRemision("262211881", 1856, d(2026, 8, 31)).isEmpty());
    }

    // ----------------------------------------------------------------- transaccion

    @Test
    @DisplayName("transaccion: obligatoria; 'mas de 4' es length() > 4 (WizardCheque.java:2193): 4 no, 5 si; sin regex")
    void transaccion() {
        assertEquals(MensajesCheque.FALTA_NRO_TRANSACCION, ReglasPanelesCheque.errorNroTransaccionObligatorio(null));
        assertEquals(MensajesCheque.FALTA_NRO_TRANSACCION, ReglasPanelesCheque.errorNroTransaccionObligatorio(""));
        assertNull(ReglasPanelesCheque.errorNroTransaccionObligatorio("1"), "lo obligatorio es solo que no este vacio");

        assertEquals("El número de transacción «1234» es demasiado corto: tiene 4 caracteres y debe tener más de 4. "
                + "Revisa que lo escribiste completo, tal como figura en el comprobante del banco.",
                ReglasPanelesCheque.errorLargoNroTransaccion("1234"));
        assertEquals("El número de transacción «1» es demasiado corto: tiene 1 carácter y debe tener más de 4. "
                + "Revisa que lo escribiste completo, tal como figura en el comprobante del banco.",
                ReglasPanelesCheque.errorLargoNroTransaccion("1"));
        assertNull(ReglasPanelesCheque.errorLargoNroTransaccion("12345"));
        assertNull(ReglasPanelesCheque.errorLargoNroTransaccion("TT262318S5JD"));
        assertNull(ReglasPanelesCheque.errorLargoNroTransaccion("14910211612"));
        assertNull(ReglasPanelesCheque.errorLargoNroTransaccion("a-b/c.d_e#f"), "no hay regex: cualquier caracter");
    }

    @Test
    @DisplayName("transaccion: se mide el texto tal cual llega, sin recortar (5 espacios cumplen, como en el legacy)")
    void transaccionSinRecortar() {
        assertNull(ReglasPanelesCheque.errorLargoNroTransaccion("     "));
        assertNull(ReglasPanelesCheque.errorLargoNroTransaccion("  ab "), "'ab' recortado seria corto, pero se mide con sus espacios (5): cumple");
        assertNotNull(ReglasPanelesCheque.errorLargoNroTransaccion(" ab "), "con 4 caracteres incluidos los espacios no cumple");
    }

    @Test
    @DisplayName("transaccion: hasta 30 (la columna es varchar(30)); 31 se rechaza en vez de truncar en silencio")
    void transaccionMaximo() {
        assertNull(ReglasPanelesCheque.errorLargoNroTransaccion(repetir('A', 30)));
        String m = ReglasPanelesCheque.errorLargoNroTransaccion(repetir('A', 31));
        assertEquals("El número de transacción «" + repetir('A', 31) + "» es demasiado largo: tiene 31 caracteres y el máximo es 30. "
                + "Revisa que sea el número de la transacción y no otro texto del comprobante.", m);
    }

    @Test
    @DisplayName("transaccion: la fecha es obligatoria y sin rango")
    void fechaTransaccion() {
        assertEquals(MensajesCheque.FALTA_FECHA_TRANSACCION, ReglasPanelesCheque.errorFechaTransaccion(null));
        assertNull(ReglasPanelesCheque.errorFechaTransaccion(d(2026, 8, 31)));
        assertNull(ReglasPanelesCheque.errorFechaTransaccion(d(2010, 1, 1)));
    }

    // ----------------------------------------------------------------- postergacion

    @Test
    @DisplayName("postergacion: la fecha es obligatoria")
    void fechaPostergacion() {
        assertEquals(MensajesCheque.FALTA_FECHA_POSTERGACION, ReglasPanelesCheque.errorFechaPostergacion(null));
        assertNull(ReglasPanelesCheque.errorFechaPostergacion(d(2025, 10, 30)));
    }

    @Test
    @DisplayName("observacion: 'mas de 2' es length() > 2 (WizardCheque.java:2247): 2 no, 3 si; hasta 200")
    void observacion() {
        assertEquals(MensajesCheque.FALTA_OBSERVACION_POSTERGACION, ReglasPanelesCheque.errorObservacionPostergacion(null));
        assertEquals(MensajesCheque.FALTA_OBSERVACION_POSTERGACION, ReglasPanelesCheque.errorObservacionPostergacion(""));
        assertEquals("La observación «ab» es demasiado corta: tiene 2 caracteres y debe tener más de 2. "
                + "Escribe el motivo de la postergación: quién la pidió y hasta cuándo.",
                ReglasPanelesCheque.errorObservacionPostergacion("ab"));
        assertEquals("La observación «a» es demasiado corta: tiene 1 carácter y debe tener más de 2. "
                + "Escribe el motivo de la postergación: quién la pidió y hasta cuándo.",
                ReglasPanelesCheque.errorObservacionPostergacion("a"));
        assertNull(ReglasPanelesCheque.errorObservacionPostergacion("abc"));
        assertNull(ReglasPanelesCheque.errorObservacionPostergacion(repetir('x', 200)));
        assertEquals("La observación es demasiado larga: tiene 201 caracteres y el máximo es 200. Acórtala.",
                ReglasPanelesCheque.errorObservacionPostergacion(repetir('x', 201)));
    }

    @Test
    @DisplayName("observacion: sin regex ni recorte; acentos, enie, saltos de linea y simbolos de los datos reales valen")
    void observacionSinRegex() {
        assertNull(ReglasPanelesCheque.errorObservacionPostergacion("cliente envió carta solicitando postergación\nmisma que se envió al grupo (ver Nº 5) - 100%"));
        assertNull(ReglasPanelesCheque.errorObservacionPostergacion("   "), "3 espacios cumplen length() > 2 (se conserva)");
    }

    // ----------------------------------------------------------------- apoyo

    @Test
    @DisplayName("mismoValorSql compara como '=' de SQL Server con varchar: sin mirar los espacios del final ni las mayusculas, pero si los del inicio")
    void mismoValorSql() {
        assertTrue(ReglasPanelesCheque.mismoValorSql("262211881", "262211881"));
        assertTrue(ReglasPanelesCheque.mismoValorSql("262211881  ", "262211881"));
        assertTrue(ReglasPanelesCheque.mismoValorSql("tt262318s5jd", "TT262318S5JD"));
        assertFalse(ReglasPanelesCheque.mismoValorSql(" 17330357", "17330357"), "el espacio inicial si cuenta");
        assertFalse(ReglasPanelesCheque.mismoValorSql("1", "2"));
        assertFalse(ReglasPanelesCheque.mismoValorSql(null, "1"));
        assertFalse(ReglasPanelesCheque.mismoValorSql("1", null));
    }
}
