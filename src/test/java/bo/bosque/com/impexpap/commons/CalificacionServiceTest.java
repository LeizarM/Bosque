package bo.bosque.com.impexpap.commons;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests de {@link CalificacionService#extraerPuntaje(String)}.
 *
 * <p>Este método decide qué termina guardado en la bitácora de calificaciones, así que sus dos
 * errores posibles no cuestan lo mismo:
 * <ul>
 *   <li><b>Perder una respuesta</b> deja un hueco en el reporte. Molesto.</li>
 *   <li><b>Inventar una</b> lo ensucia, y encima le atribuye a un chofer un puntaje que el
 *       cliente nunca dio. Eso es un dato falso en un reporte de desempeño.</li>
 * </ul>
 * Por eso los casos de "NO es una calificación" son mayoría acá: son los que importa blindar.
 */
class CalificacionServiceTest {

    // ============================================================
    // Lo que SÍ es una calificación
    // ============================================================

    @Test
    @DisplayName("el número solo, que es lo que pide el mensaje")
    void numeroSolo() {
        assertEquals(Integer.valueOf(5), CalificacionService.extraerPuntaje("5"));
        assertEquals(Integer.valueOf(1), CalificacionService.extraerPuntaje("1"));
        assertEquals(Integer.valueOf(3), CalificacionService.extraerPuntaje("  3  "));
        assertEquals(Integer.valueOf(4), CalificacionService.extraerPuntaje("4!"));
        assertEquals(Integer.valueOf(2), CalificacionService.extraerPuntaje("2."));
    }

    @Test
    @DisplayName("formas que la gente escribe igual aunque no se las pida")
    void formasHabituales() {
        assertEquals(Integer.valueOf(5), CalificacionService.extraerPuntaje("cinco"));
        assertEquals(Integer.valueOf(5), CalificacionService.extraerPuntaje("5/5"));
        assertEquals(Integer.valueOf(4), CalificacionService.extraerPuntaje("un 4"));
        assertEquals(Integer.valueOf(4), CalificacionService.extraerPuntaje("4 estrellas"));
        assertEquals(Integer.valueOf(5), CalificacionService.extraerPuntaje("le doy 5"));
        assertEquals(Integer.valueOf(5), CalificacionService.extraerPuntaje("5 gracias"));
    }

    // ============================================================
    // Lo que NO es una calificación — el corazón del asunto
    // ============================================================

    @Test
    @DisplayName("mensajes cortos con un número que hablan de otra cosa")
    void numeroQueNoEsPuntaje() {
        // Todos tienen menos de 30 caracteres y un único dígito de la escala: antes del
        // filtro de palabras, los seis quedaban guardados como calificación.
        assertNull(CalificacionService.extraerPuntaje("estoy en el piso 3"));
        assertNull(CalificacionService.extraerPuntaje("llego en 5 min"));
        assertNull(CalificacionService.extraerPuntaje("son 4 cajas?"));
        assertNull(CalificacionService.extraerPuntaje("el lunes 2"));
        assertNull(CalificacionService.extraerPuntaje("faltaron 2 paquetes"));
        assertNull(CalificacionService.extraerPuntaje("dejalo en la puerta 5"));
    }

    @Test
    @DisplayName("números que no pertenecen a la escala")
    void fueraDeEscala() {
        assertNull(CalificacionService.extraerPuntaje("0"));
        assertNull(CalificacionService.extraerPuntaje("6"));
        assertNull(CalificacionService.extraerPuntaje("10"));
        assertNull(CalificacionService.extraerPuntaje("2024"));
        assertNull(CalificacionService.extraerPuntaje("71234567"));
    }

    @Test
    @DisplayName("ambigüedad: no se adivina")
    void ambiguo() {
        assertNull(CalificacionService.extraerPuntaje("no fue 5 fue 2"));
        assertNull(CalificacionService.extraerPuntaje("3 y 4"));
        assertNull(CalificacionService.extraerPuntaje("entre 2 y 3"));
    }

    @Test
    @DisplayName("texto sin número")
    void sinNumero() {
        assertNull(CalificacionService.extraerPuntaje("excelente"));
        assertNull(CalificacionService.extraerPuntaje("todo bien gracias"));
        assertNull(CalificacionService.extraerPuntaje("ok"));
        assertNull(CalificacionService.extraerPuntaje(""));
        assertNull(CalificacionService.extraerPuntaje("   "));
        assertNull(CalificacionService.extraerPuntaje(null));
    }

    @Test
    @DisplayName("un reclamo largo con un número adentro no es un puntaje")
    void reclamoLargo() {
        assertNull(CalificacionService.extraerPuntaje(
                "buenas, me llegaron 3 paquetes pero en la factura decia 5, quien me puede ayudar"));
    }
}
