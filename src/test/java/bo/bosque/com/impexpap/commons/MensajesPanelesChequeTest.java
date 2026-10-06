package bo.bosque.com.impexpap.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Los mensajes de los paneles del detalle del cheque (notas de remision, transacciones, postergaciones y el PDF de la
 * postergacion): cada uno dice QUE falla, POR QUE y QUE hacer, con los datos concretos, en espanol neutro con tuteo y sin las
 * frases escuetas del legacy.
 */
class MensajesPanelesChequeTest {

    private static List<String> todos() {
        return Arrays.asList(
                MensajesCheque.FALTA_CHEQUE_DEL_PANEL,
                MensajesCheque.FALTA_NOTA_REMISION, MensajesCheque.notaRemisionNoValida("12a", Pattern.compile("[0-9 ]")),
                MensajesCheque.FALTA_NRO_FACTURA, MensajesCheque.nroFacturaNoPositivo(0), MensajesCheque.nroFacturaMuyLargo(1_000_000, 999_999),
                MensajesCheque.FALTA_FECHA_FACTURA, MensajesCheque.FALTA_NOTA_A_ELIMINAR, MensajesCheque.notaNoEncontrada(18129, "262211881"),
                MensajesCheque.notaEliminada(1, "262211881"), MensajesCheque.notaEliminada(2, "262211881"),
                MensajesCheque.FALTA_NRO_TRANSACCION, MensajesCheque.nroTransaccionCorto("123", 3), MensajesCheque.nroTransaccionLargo("x", 31, 30),
                MensajesCheque.FALTA_BANCO_DE_TRANSACCION, MensajesCheque.FALTA_FECHA_TRANSACCION, MensajesCheque.FALTA_TRANSACCION_A_ELIMINAR,
                MensajesCheque.transaccionNoEncontrada(18129, "TT262318S5JD"), MensajesCheque.transaccionEliminada(1, "x"),
                MensajesCheque.transaccionEliminada(3, "x"),
                MensajesCheque.FALTA_FECHA_POSTERGACION, MensajesCheque.FALTA_OBSERVACION_POSTERGACION,
                MensajesCheque.observacionPostergacionCorta("ab", 2), MensajesCheque.observacionPostergacionLarga(201, 200),
                MensajesCheque.FALTA_POSTERGACION, MensajesCheque.postergacionNoEncontrada(40), MensajesCheque.postergacionDeOtroCheque(40, 18129),
                MensajesCheque.FALTA_POSTERGACION_PDF, MensajesCheque.codPostergacionPdfNoValido("x"),
                MensajesCheque.FALTA_ARCHIVO_PDF_POSTERGACION, MensajesCheque.archivoNoEsPdfPostergacion("a.png"),
                MensajesCheque.archivoPdfPostergacionVacio("a.pdf"), MensajesCheque.CARPETA_PDF_POSTERGACION_NO_CONFIGURADA,
                MensajesCheque.carpetaPdfPostergacionNoDisponible("/x", "no se pudo crear"), MensajesCheque.pdfPostergacionNoEncontrado(40),
                MensajesCheque.noSePudoGuardarPdfPostergacion(40), MensajesCheque.noSePudoLeerPdfPostergacion(40));
    }

    @Test
    @DisplayName("hablan de tu (nada de voseo), son frases completas y no conservan las frases escuetas del legacy")
    void tuteoYFrasesCompletas() {
        Pattern voseo = Pattern.compile("(?i)\\b(tenés|podés|querés|sos|elegí|escribí|actualizá|avisá|revisá|cargá|pedile|volvé|usá|ingresá|seleccioná|probá)\\b");
        for (String m : todos()) {
            assertFalse(voseo.matcher(m).find(), "voseo en: " + m);
            for (String vieja : new String[] {"Verifique", "Ingrese", "Faltan Parametros", "Parametros Erroneos", "NO fue Almacenado",
                    "No se Elimino", "Cargue el Nro", "Accion Invalida", "Favor"}) {
                assertFalse(m.contains(vieja), "«" + vieja + "» sigue en: " + m);
            }
            assertTrue(m.endsWith(".") || m.endsWith("»."), "cada mensaje es una frase completa: " + m);
        }
    }

    @Test
    @DisplayName("cada mensaje de dato concreto lo nombra: la nota, la transaccion, la postergacion y el cheque")
    void nombranElDato() {
        assertTrue(MensajesCheque.notaNoEncontrada(18129, " 262211881 ").contains("cheque 18129")
                && MensajesCheque.notaNoEncontrada(18129, " 262211881 ").contains("«262211881»"));
        assertTrue(MensajesCheque.transaccionNoEncontrada(18129, "TT262318S5JD").contains("«TT262318S5JD»"));
        assertTrue(MensajesCheque.postergacionNoEncontrada(40).contains("postergación 40"));
        assertTrue(MensajesCheque.postergacionDeOtroCheque(40, 18129).contains("postergación 40")
                && MensajesCheque.postergacionDeOtroCheque(40, 18129).contains("cheque 18129"));
        assertTrue(MensajesCheque.pdfPostergacionNoEncontrado(40).contains("postergación 40"));
        assertTrue(MensajesCheque.codPostergacionPdfNoValido("../1").contains("«../1»"));
        for (String m : Arrays.asList(MensajesCheque.notaNoEncontrada(1, "1"), MensajesCheque.transaccionNoEncontrada(1, "1"),
                MensajesCheque.postergacionNoEncontrada(1), MensajesCheque.pdfPostergacionNoEncontrado(1))) {
            assertTrue(m.contains("actualiza la pantalla") || m.contains("Usa «Cargar PDF»"), "dice que hacer: " + m);
        }
    }

    @Test
    @DisplayName("los numeros de factura, de transaccion y de observacion dicen cuanto escribiste y cual es el limite")
    void dicenElLimite() {
        assertTrue(MensajesCheque.nroFacturaMuyLargo(1_234_567, 999_999).contains("tiene 7 dígitos")
                && MensajesCheque.nroFacturaMuyLargo(1_234_567, 999_999).contains("hasta 6")
                && MensajesCheque.nroFacturaMuyLargo(1_234_567, 999_999).contains("999999"));
        assertTrue(MensajesCheque.nroTransaccionCorto("12", 2).contains("tiene 2 caracteres y debe tener más de 4"));
        assertTrue(MensajesCheque.nroTransaccionLargo("x", 40, 30).contains("tiene 40 caracteres y el máximo es 30"));
        assertTrue(MensajesCheque.observacionPostergacionCorta("a", 1).contains("tiene 1 carácter y debe tener más de 2"));
        assertTrue(MensajesCheque.observacionPostergacionLarga(250, 200).contains("tiene 250 caracteres y el máximo es 200"));
    }

    @Test
    @DisplayName("la carpeta de los PDF de postergaciones: no configurada nombra la propiedad y la variable; no disponible dice la ruta y que revisen ruta y permisos")
    void carpeta() {
        String sin = MensajesCheque.CARPETA_PDF_POSTERGACION_NO_CONFIGURADA;
        assertTrue(sin.contains("cheques.postergacion.pdf.dir") && sin.contains("POSTERGACIONES_PDF_DIR") && sin.contains("Avisa a sistemas"), sin);

        String no = MensajesCheque.carpetaPdfPostergacionNoDisponible("/mnt/postergaciones", "no se pudo crear");
        assertTrue(no.contains("«/mnt/postergaciones»") && no.contains("no se pudo crear") && no.contains("cheques.postergacion.pdf.dir"), no);
        assertFalse(no.contains("no la crea por su cuenta"), "ya se crea sola al subir: " + no);
        assertTrue(no.contains("postergaciones"));
        // el de los cheques no se confunde con este
        assertFalse(MensajesCheque.CARPETA_PDF_NO_CONFIGURADA.contains("POSTERGACIONES_PDF_DIR"));
    }

    @Test
    @DisplayName("el boton de 'Nuevo' y el de eliminar nota tienen su explicacion en el 403")
    void botones() {
        assertEquals("No tienes permiso para registrar notas de remisión, transacciones bancarias y postergaciones de un cheque. "
                + "Tu usuario no tiene asignado el botón btnNuevoNRCH; pídele al administrador que te lo asigne.",
                MensajesCheque.sinPermiso("btnNuevoNRCH"));
        assertEquals("No tienes permiso para eliminar notas de remisión de un cheque. "
                + "Tu usuario no tiene asignado el botón btnEliminarNRCH; pídele al administrador que te lo asigne.",
                MensajesCheque.sinPermiso("btnEliminarNRCH"));
    }

    @Test
    @DisplayName("la nota de remision nombra como sobrantes exactamente lo que el regex rechaza (letras, simbolos); espacio se llama 'espacio'")
    void sobrantes() {
        String m = MensajesCheque.notaRemisionNoValida("12 a/5", ReglasPanelesCheque.NOTA_REMISION_CAR);
        assertTrue(m.contains("(«a», «/»)"), m);
        assertFalse(m.contains("(espacio") || m.contains(", espacio"), "el espacio SI se acepta, no sobra: " + m);
    }
}
