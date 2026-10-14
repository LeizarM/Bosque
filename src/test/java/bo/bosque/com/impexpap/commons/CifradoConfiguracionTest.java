package bo.bosque.com.impexpap.commons;

import bo.bosque.com.impexpap.config.SpBusinessException;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** AES-256-GCM de los parámetros secretos: ida y vuelta, IV distinto, y qué pasa sin clave o con otra clave. */
class CifradoConfiguracionTest {

    private static String clave(int bytes, int semilla) {
        byte[] b = new byte[bytes];
        for (int i = 0; i < bytes; i++) b[i] = (byte) (i * 7 + semilla);
        return Base64.getEncoder().encodeToString(b);
    }

    @Test
    void loCifradoSeDescifraIgualYNoSeLeeEnLaBase() {
        CifradoConfiguracion c = new CifradoConfiguracion(clave(32, 1));

        String guardado = c.cifrar("clave con espacios y ñ");

        assertFalse(guardado.contains("clave"));
        assertEquals("clave con espacios y ñ", c.descifrar(guardado));
    }

    @Test
    void cadaCifradoUsaOtroIv() {
        CifradoConfiguracion c = new CifradoConfiguracion(clave(32, 1));

        assertNotEquals(c.cifrar("misma"), c.cifrar("misma"));
    }

    @Test
    void sinClaveAvisaQueFaltaConfigurarla() {
        CifradoConfiguracion c = new CifradoConfiguracion("");

        SpBusinessException e = assertThrows(SpBusinessException.class, () -> c.cifrar("x"));
        assertEquals(CifradoConfiguracion.MSG_SIN_CLAVE, e.getMessage());
    }

    @Test
    void unaClaveDe64BytesTambienSirve() {
        CifradoConfiguracion c = new CifradoConfiguracion(clave(64, 3));

        assertEquals("secreto", c.descifrar(c.cifrar("secreto")));
    }

    @Test
    void unaClaveCortaNoArranca() {
        assertThrows(IllegalStateException.class, () -> new CifradoConfiguracion(clave(16, 1)));
    }

    @Test
    void unaClaveDeOtroLargoNoArrancaYDiceComoGenerarla() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> new CifradoConfiguracion("bosque2026"));
        assertTrue(e.getMessage().startsWith(CifradoConfiguracion.MSG_CLAVE_MAL_FORMADA));
        assertTrue(e.getMessage().contains("7 bytes"));
    }

    @Test
    void unaContrasenaConSimbolosTampocoArrancaYDiceLoMismo() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> new CifradoConfiguracion("Clave@2026!"));
        assertTrue(e.getMessage().startsWith(CifradoConfiguracion.MSG_CLAVE_MAL_FORMADA));
    }

    @Test
    void conOtraClaveNoSePuedeLeer() {
        String guardado = new CifradoConfiguracion(clave(32, 1)).cifrar("secreto");

        assertThrows(IllegalStateException.class, () -> new CifradoConfiguracion(clave(32, 2)).descifrar(guardado));
    }
}
