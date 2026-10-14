package bo.bosque.com.impexpap.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/** leer() verifica la firma una vez: de sus claims salen el login y la identidad; lo inválido da null. */
class JwtProviderTest {

    private static final byte[] CLAVE = relleno((byte) 7);
    private final JwtProvider proveedor = new JwtProvider(Base64.getEncoder().encodeToString(CLAVE), 3600);

    private static byte[] relleno(byte b) {
        byte[] clave = new byte[64];
        Arrays.fill(clave, b);
        return clave;
    }

    private static String firmar(byte[] clave, long venceEnMs) {
        return Jwts.builder().setSubject("jperez").setId("5")
                .claim("codEmpleado", 7).claim("codEmpresa", 1).claim("tipoUsuario", "adm").claim("sid", 33L)
                .setExpiration(new Date(System.currentTimeMillis() + venceEnMs))
                .signWith(SignatureAlgorithm.HS512, clave).compact();
    }

    @Test
    void unTokenValidoDaElLoginYLaIdentidadConSuSesion() {
        Claims c = proveedor.leer(firmar(CLAVE, 60_000));

        assertNotNull(c);
        assertEquals("jperez", c.getSubject());
        DatosToken d = proveedor.datosDe(c);
        assertEquals(5, d.getCodUsuario());
        assertEquals(7, d.getCodEmpleado());
        assertEquals(33, d.getIdSesion());
    }

    @Test
    void unTokenVencidoConOtraFirmaOMalFormadoDaNull() {
        assertNull(proveedor.leer(firmar(CLAVE, -60_000)));
        assertNull(proveedor.leer(firmar(relleno((byte) 9), 60_000)));
        assertNull(proveedor.leer("no-es-un-token"));
        assertNull(proveedor.leer(""));
    }
}
