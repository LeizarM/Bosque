package bo.bosque.com.impexpap.commons;

import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** El emisor se arma con lo que dice trec_configuracion: servidor, casilla y el cifrado elegido. */
class CorreoRecuperacionTest {

    private static Map<String, String> config(String seguridad, String puerto) {
        Map<String, String> c = new HashMap<>();
        c.put("smtpServidor", "mail.esppapel.com");
        c.put("smtpPuerto", puerto);
        c.put("smtpUsuario", "bosque@esppapel.com");
        c.put("smtpClave", "la-clave");
        c.put("smtpSeguridad", seguridad);
        return c;
    }

    @Test
    void starttlsEnElPuerto587() {
        JavaMailSenderImpl s = CorreoRecuperacion.emisor(config("STARTTLS", "587"));

        assertEquals("mail.esppapel.com", s.getHost());
        assertEquals(587, s.getPort());
        assertEquals("bosque@esppapel.com", s.getUsername());
        assertEquals("la-clave", s.getPassword());
        assertEquals("true", s.getJavaMailProperties().getProperty("mail.smtp.starttls.enable"));
        assertNull(s.getJavaMailProperties().getProperty("mail.smtp.ssl.enable"));
    }

    @Test
    void sslEnElPuerto465() {
        JavaMailSenderImpl s = CorreoRecuperacion.emisor(config("SSL", "465"));

        assertEquals(465, s.getPort());
        assertEquals("true", s.getJavaMailProperties().getProperty("mail.smtp.ssl.enable"));
        assertNull(s.getJavaMailProperties().getProperty("mail.smtp.starttls.enable"));
    }

    @Test
    void sinCifrado() {
        JavaMailSenderImpl s = CorreoRecuperacion.emisor(config("NINGUNA", "25"));

        assertNull(s.getJavaMailProperties().getProperty("mail.smtp.ssl.enable"));
        assertNull(s.getJavaMailProperties().getProperty("mail.smtp.starttls.enable"));
    }
}
