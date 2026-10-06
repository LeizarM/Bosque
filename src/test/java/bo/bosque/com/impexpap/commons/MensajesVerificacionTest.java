package bo.bosque.com.impexpap.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Los textos de error de Verificar Cheques: que ninguno sea ambiguo (que nombre el dato y diga que hacer) y que no
 * contradigan las reglas. Sin base de datos ni Spring.
 */
class MensajesVerificacionTest {

    /** Verbos con los que un mensaje le dice al usuario que hacer (tuteo). */
    private static final List<String> QUE_HACER = Arrays.asList(
            "Vuelve", "Elige", "Actualiza", "anúlala", "Verifica", "Intenta");

    /** Todos los mensajes del modulo, los fijos y los que llevan datos (con datos de ejemplo). */
    private static List<String> todos() {
        List<String> m = new ArrayList<>();
        m.add(MensajesVerificacion.SIN_DATOS);
        m.add(MensajesVerificacion.FALTA_FECHA);
        m.add(MensajesVerificacion.FALTA_BANCO);
        m.add(MensajesVerificacion.FALTA_CHEQUE);
        m.add(MensajesVerificacion.FALTA_CHEQUE_A_PREPARAR);
        m.add(MensajesVerificacion.FALTA_VERIFICACION_A_ANULAR);
        m.add(MensajesVerificacion.bancoNoEncontrado(9L));
        m.add(MensajesVerificacion.chequeNoEncontrado(321L));
        m.add(MensajesVerificacion.chequeYaVerificado(321L));
        m.add(MensajesVerificacion.chequeCerrado("5551", 321L));
        m.add(MensajesVerificacion.verificacionNoEncontrada(88L));
        m.add(MensajesVerificacion.estadoDeChequeNoValido("XYZ", Arrays.asList("PEN", "CER")));
        return m;
    }

    @Test
    @DisplayName("ningun mensaje es ambiguo: frase completa, sin saltos de linea, y dice que hacer")
    void ningunoEsAmbiguo() {
        for (String m : todos()) {
            assertTrue(m.length() > 40, "demasiado corto para explicar algo: " + m);
            assertTrue(Character.isUpperCase(m.charAt(0)), "empieza con mayuscula: " + m);
            assertTrue(m.endsWith("."), "termina en punto: " + m);
            assertFalse(m.contains("\n"), "un mensaje es una sola linea (varios errores los junta el servicio): " + m);
            assertFalse(m.toLowerCase().contains("registro no disponible"), "el texto ambiguo del legacy: " + m);
            assertFalse(m.toLowerCase().contains("accion invalida"), "el texto ambiguo del legacy: " + m);
            assertTrue(QUE_HACER.stream().anyMatch(v -> m.toLowerCase().contains(v.toLowerCase())), "dice que hacer (" + QUE_HACER + "): " + m);
            // tuteo, nunca voseo
            assertFalse(m.contains("tenés") || m.contains("elegí") || m.contains("podés"), m);
        }
    }

    @Test
    @DisplayName("los mensajes con datos nombran el dato concreto (codigo, numero de cheque, estado)")
    void nombranElDato() {
        assertTrue(MensajesVerificacion.bancoNoEncontrado(9L).contains("código 9"));
        assertTrue(MensajesVerificacion.chequeNoEncontrado(321L).contains("cheque 321"));
        assertTrue(MensajesVerificacion.chequeYaVerificado(321L).contains("cheque 321"));
        String cerrado = MensajesVerificacion.chequeCerrado("5551", 321L);
        assertTrue(cerrado.contains("número 5551") && cerrado.contains("código 321") && cerrado.contains("CERRADO"), cerrado);
        assertTrue(MensajesVerificacion.verificacionNoEncontrada(88L).contains("verificación 88"));
        String estado = MensajesVerificacion.estadoDeChequeNoValido("XYZ", Arrays.asList("PEN", "CER"));
        assertTrue(estado.contains("«XYZ»") && estado.contains("PEN o CER") && estado.contains("«Todos»"), estado);
    }

    @Test
    @DisplayName("un cheque sin numero se nombra (sin número) en vez de dejar un hueco")
    void chequeSinNumero() {
        assertTrue(MensajesVerificacion.chequeCerrado(null, 5L).contains("(sin número)"));
        assertTrue(MensajesVerificacion.chequeCerrado("  ", 5L).contains("(sin número)"));
    }

    @Test
    @DisplayName("el mensaje de una verificacion ya vigente explica la regla (una sola valida) y que hacer (anular)")
    void yaVerificadoExplicaLaRegla() {
        String m = MensajesVerificacion.chequeYaVerificado(7L);
        assertTrue(m.contains("solo puede tener una"), m);
        assertTrue(m.contains("«Cancelar»"), m);
    }

    @Test
    @DisplayName("ya estaba anulada: dice que no se cambio nada (no 'anulada' a secas)")
    void yaAnulada() {
        assertEquals("Esta verificación ya estaba anulada: no hay nada que cambiar.", MensajesVerificacion.YA_ESTABA_ANULADA);
    }
}
