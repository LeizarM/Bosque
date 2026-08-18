package bo.bosque.com.impexpap.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests de {@link Fmt#dias(double)}.
 *
 * <p>Este método decide el número que RR.HH. lee en pantalla y compara contra el ERP legacy
 * con tolerancia 0 (criterios de aceptación #3 y #4). Un decimal de más o de menos acá se ve
 * como una diferencia de saldo, no como un problema de formato.
 */
class FmtTest {

    @Test
    @DisplayName("los casos de la especificacion")
    void casosDeLaSpec() {
        assertEquals("0 días",    Fmt.dias(0));
        assertEquals("1 día",     Fmt.dias(1));
        assertEquals("1,5 días",  Fmt.dias(1.5));
        assertEquals("12,5 días", Fmt.dias(12.5));
        assertEquals("30 días",   Fmt.dias(30));
        assertEquals("-16 días",  Fmt.dias(-16));
    }

    @Test
    @DisplayName("sin ceros a la derecha: 8.0 no es '8,00'")
    void sinCerosALaDerecha() {
        assertEquals("8 días",    Fmt.dias(8.0));
        assertEquals("12,9 días", Fmt.dias(12.90));
        assertEquals("0 días",    Fmt.dias(0.0));
    }

    @Test
    @DisplayName("hasta 2 decimales, redondeando")
    void dosDecimales() {
        assertEquals("12,35 días", Fmt.dias(12.345));
        assertEquals("0,44 días",  Fmt.dias(0.4375));   // el puente de sabado, 08:30-12:30
        assertEquals("0 días",     Fmt.dias(0.001));
    }

    @Test
    @DisplayName("el singular tambien vale para -1")
    void singular() {
        assertEquals("-1 día", Fmt.dias(-1));
        assertEquals("2 días", Fmt.dias(2));
    }

    @Test
    @DisplayName("el ruido binario del float de SQL Server no se filtra a la pantalla")
    void ruidoBinario() {
        assertEquals("12,5 días", Fmt.dias(12.500000000000002));
        assertEquals("0,1 días",  Fmt.dias(0.1 + 0.2 - 0.2));
    }
}
