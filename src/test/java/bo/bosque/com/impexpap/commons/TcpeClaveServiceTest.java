package bo.bosque.com.impexpap.commons;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Generador y hash de claves del módulo de exámenes. Se usa BCrypt de costo 4 para que la prueba no tarde: lo que
 * se verifica es la lógica (alfabeto, largo, orden, paralelo), no el costo, que fija {@code TcpeSeguridadConfig}.
 */
class TcpeClaveServiceTest {

    private TcpeClaveService servicio;

    @BeforeEach
    void preparar() {
        servicio = new TcpeClaveService(new BCryptPasswordEncoder(4));
    }

    @AfterEach
    void cerrar() {
        servicio.cerrar();
    }

    @Test
    @DisplayName("la clave tiene 10 caracteres alfanuméricos y nunca 0, O, 1, l ni I")
    void alfabetoYLargo() {
        Set<String> vistas = new HashSet<>();
        for (int i = 0; i < 5000; i++) {
            String clave = servicio.generar();
            assertEquals(TcpeClaveService.LARGO, clave.length());
            assertTrue(clave.matches("[A-Za-z2-9]{10}"), clave);
            for (char prohibido : new char[] { '0', 'O', '1', 'l', 'I' }) {
                assertTrue(clave.indexOf(prohibido) < 0, "la clave " + clave + " tiene " + prohibido);
            }
            vistas.add(clave);
        }
        // 57^10 combinaciones: una repetida en 5000 sería una señal de un generador roto.
        assertEquals(5000, vistas.size());
    }

    @Test
    @DisplayName("el alfabeto tiene los 57 símbolos esperados, sin repetidos")
    void alfabeto() {
        String a = TcpeClaveService.ALFABETO;
        assertEquals(57, a.length());
        assertEquals(57, a.chars().distinct().count());
        assertFalse(a.matches(".*[0O1lI].*"));
    }

    @Test
    @DisplayName("se usan todos los símbolos del alfabeto (la distribución no está sesgada a un subconjunto)")
    void usaTodoElAlfabeto() {
        Set<Character> usados = new HashSet<>();
        for (int i = 0; i < 2000; i++) {
            for (char c : servicio.generar().toCharArray()) usados.add(c);
        }
        assertEquals(TcpeClaveService.ALFABETO.length(), usados.size());
    }

    @Test
    @DisplayName("los hash se calculan en el pool, en paralelo, y respetan el orden de las claves")
    void hashEnParaleloYEnOrden() {
        Set<String> hilos = ConcurrentHashMap.newKeySet();
        PasswordEncoder base = new BCryptPasswordEncoder(4);
        PasswordEncoder espia = new PasswordEncoder() {
            @Override
            public String encode(CharSequence raw) {
                hilos.add(Thread.currentThread().getName());
                return base.encode(raw);
            }

            @Override
            public boolean matches(CharSequence raw, String encoded) {
                return base.matches(raw, encoded);
            }
        };
        TcpeClaveService conEspia = new TcpeClaveService(espia);
        try {
            List<TcpeClaveService.Clave> claves = conEspia.generarConHash(12);
            assertEquals(12, claves.size());
            for (TcpeClaveService.Clave c : claves) {
                assertTrue(TcpeClaveService.esHash(c.getHash()), c.getHash());
                assertTrue(base.matches(c.getPlano(), c.getHash()), "el hash no corresponde a su clave");
            }
            assertTrue(hilos.stream().allMatch(h -> h.startsWith("tcpe-bcrypt-")), hilos.toString());
        } finally {
            conEspia.cerrar();
        }
    }

    @Test
    @DisplayName("coincide acepta el hash y la clave plana heredada del legacy; nada guardado no coincide")
    void coincide() {
        String hash = servicio.hash("Abc2345xyz");
        assertTrue(servicio.coincide("Abc2345xyz", hash));
        assertFalse(servicio.coincide("otra", hash));

        assertTrue(servicio.coincide("clave-legacy", "clave-legacy"));
        assertFalse(servicio.coincide("clave-legacy", "Clave-legacy"));

        assertFalse(servicio.coincide("x", null));
        assertFalse(servicio.coincide("x", ""));
        assertFalse(servicio.coincide(null, "x"));
    }

    @Test
    @DisplayName("esHash reconoce un BCrypt y rechaza textos planos que empiezan parecido")
    void esHash() {
        assertTrue(TcpeClaveService.esHash(servicio.hash("x")));
        for (String s : Arrays.asList(null, "", "123456", "$2a$", "$2a$14$corto", "$2a$14$" + "x" + "a")) {
            assertFalse(TcpeClaveService.esHash(s), String.valueOf(s));
        }
    }
}
