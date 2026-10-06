package bo.bosque.com.impexpap.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Las reglas de formato de Verificar Cheques, fijadas contra lo que hace la expresion regular del legacy
 * ({@code VerificacionDepositoManagedBean.validarObservacion}), no contra lo que dice su mensaje.
 */
class ReglasVerificacionTest {

    private static String repetir(char c, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(c);
        return sb.toString();
    }

    @Test
    @DisplayName("observacion valida: letras sin tilde, digitos, espacio, apostrofe, coma y punto; vacia o ausente tambien")
    void observacionValida() {
        assertNull(ReglasVerificacion.errorObservacion(null));
        assertNull(ReglasVerificacion.errorObservacion(""), "JSF no valida un campo vacio");
        assertNull(ReglasVerificacion.errorObservacion(" "));
        assertNull(ReglasVerificacion.errorObservacion("Deposito verificado"));
        assertNull(ReglasVerificacion.errorObservacion("Cheque 123 OK"));
        assertNull(ReglasVerificacion.errorObservacion("O'Neil, Juan. 2da vez"));
        assertNull(ReglasVerificacion.errorObservacion("a,b.c"));
    }

    @Test
    @DisplayName("observacion invalida: tilde, ene, punto y coma, dos puntos, guion, barra, parentesis, salto de linea")
    void observacionInvalida() {
        for (String malo : new String[] {"Depósito", "año", "Ñandú", "a;b", "a:b", "a-b", "a/b", "(ok)", "a_b", "a\nb", "a\r\nb", "100%", "a\tb"}) {
            assertNotNull(ReglasVerificacion.errorObservacion(malo), "debe rechazar: " + malo.replace("\n", "\\n"));
        }
    }

    @Test
    @DisplayName("el limite es 50 caracteres: 50 pasa, 51 no")
    void observacionLargo() {
        assertNull(ReglasVerificacion.errorObservacion(repetir('a', 50)));
        String m = ReglasVerificacion.errorObservacion(repetir('a', 51));
        assertNotNull(m);
        assertTrue(m.contains("es demasiado largo (51 caracteres)"), m);
        assertTrue(m.contains("hasta 50 caracteres"), m);
    }

    @Test
    @DisplayName("el literal del legacy se copio tal cual: el patron admite exactamente a-z A-Z 0-9 espacio ' , .")
    void conjuntoExacto() {
        Pattern car = ReglasVerificacion.OBSERVACION_CAR;
        int admitidos = 0;
        for (int c = 0; c < 0x250; c++) {
            String uno = String.valueOf((char) c);
            boolean porElPatron = ReglasVerificacion.OBSERVACION.matcher(uno).matches();
            assertEquals(porElPatron, car.matcher(uno).matches(),
                    "el caracter de ayuda del mensaje no coincide con el patron en U+" + Integer.toHexString(c));
            if (porElPatron) admitidos++;
        }
        assertEquals(26 + 26 + 10 + 1 + 3, admitidos, "letras, digitos, espacio y los tres signos ' , .");
        assertTrue(ReglasVerificacion.OBSERVACION.matcher("'").matches());
        assertTrue(ReglasVerificacion.OBSERVACION.matcher(",").matches());
        assertTrue(ReglasVerificacion.OBSERVACION.matcher(".").matches());
        assertFalse(ReglasVerificacion.OBSERVACION.matcher("-").matches(), "en el literal no hay ningun guion: no es un rango");
        assertFalse(ReglasVerificacion.OBSERVACION.matcher(";").matches());
        assertEquals("^[a-zA-Z0-9  ' '',''.' ]{0,50}", ReglasVerificacion.OBSERVACION.pattern(), "el literal del legacy, sin tocar");
    }

    @Test
    @DisplayName("el mensaje de formato nombra como sobrantes exactamente lo que el patron rechaza")
    void mensajeNombraLoQueSobra() {
        String m = ReglasVerificacion.errorObservacion("Depósito: ok; año");
        assertTrue(m.startsWith("El texto de la observación «Depósito: ok; año» no es válido"), m);
        assertTrue(m.contains("«ó»") && m.contains("«:»") && m.contains("«;»") && m.contains("«ñ»"), m);
        // lo permitido NO se nombra como sobrante
        assertFalse(m.contains("«D»") || m.contains("«p»") || m.contains("«k»"), m);
        assertTrue(m.contains("Solo acepta letras sin tilde ni ñ, números, espacios y los signos ' , ."), m);
    }

    @Test
    @DisplayName("un salto de linea se nombra como ¶ y el mensaje sigue siendo UNA linea (no se parte en dos)")
    void saltoDeLinea() {
        String m = ReglasVerificacion.errorObservacion("linea 1\nlinea 2");
        assertFalse(m.contains("\n"), m);
        assertFalse(m.contains("\r"), m);
        assertTrue(m.contains("«¶»"), m);
        assertTrue(m.contains("linea 1¶linea 2"), m);
    }

    @Test
    @DisplayName("cheque cerrado: la descripcion CERRADO (sin distinguir mayusculas ni espacios), como el JSF")
    void cerrado() {
        assertTrue(ReglasVerificacion.esCerrado("CERRADO"));
        assertTrue(ReglasVerificacion.esCerrado(" cerrado "));
        assertFalse(ReglasVerificacion.esCerrado("PENDIENTE"));
        assertFalse(ReglasVerificacion.esCerrado("CER"), "es la descripcion la que se compara, no el codigo");
        assertFalse(ReglasVerificacion.esCerrado(null));
        assertFalse(ReglasVerificacion.esCerrado(""));
    }
}
