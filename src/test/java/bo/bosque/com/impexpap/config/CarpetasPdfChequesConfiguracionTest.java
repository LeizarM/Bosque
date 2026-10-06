package bo.bosque.com.impexpap.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.support.ResourcePropertySource;

/**
 * Las carpetas de los PDF de cheques y de postergaciones salen, por defecto, de {@code uploads.dir} (ruta relativa a la
 * aplicacion, como Depositos y Pagos al exterior): nada de unidades de Windows ni de montajes que configurar. Se lee el
 * {@code application.properties} REAL, sin variables de entorno ni propiedades del equipo que lo contaminen.
 */
class CarpetasPdfChequesConfiguracionTest {

    private static PropertySourcesPropertyResolver resolver(Map<String, Object> extra) throws IOException {
        MutablePropertySources fuentes = new MutablePropertySources();
        fuentes.addFirst(new MapPropertySource("prueba", extra));
        fuentes.addLast(new ResourcePropertySource("classpath:application.properties"));
        return new PropertySourcesPropertyResolver(fuentes);
    }

    private static Map<String, Object> con(String clave, String valor) {
        Map<String, Object> m = new HashMap<>();
        m.put(clave, valor);
        return m;
    }

    @Test
    @DisplayName("desarrollo (uploads.dir = ./uploads): los PDF van a ./uploads/cheques y ./uploads/postergacionCheques")
    void desarrollo() throws IOException {
        PropertySourcesPropertyResolver r = resolver(con("uploads.dir", "./uploads"));

        assertEquals("./uploads/cheques", r.getProperty("cheques.pdf.dir"));
        assertEquals("./uploads/postergacionCheques", r.getProperty("cheques.postergacion.pdf.dir"));
    }

    @Test
    @DisplayName("produccion (uploads.dir = /app/uploads, el volumen persistente): los PDF van dentro de ese volumen")
    void produccion() throws IOException {
        PropertySourcesPropertyResolver r = resolver(con("uploads.dir", "/app/uploads"));

        assertEquals("/app/uploads/cheques", r.getProperty("cheques.pdf.dir"));
        assertEquals("/app/uploads/postergacionCheques", r.getProperty("cheques.postergacion.pdf.dir"));
    }

    @Test
    @DisplayName("sin uploads.dir (otro perfil o una prueba): ./uploads, nunca un error de placeholder sin resolver")
    void sinUploadsDir() throws IOException {
        PropertySourcesPropertyResolver r = resolver(Collections.<String, Object>emptyMap());

        assertEquals("./uploads/cheques", r.getProperty("cheques.pdf.dir"));
        assertEquals("./uploads/postergacionCheques", r.getProperty("cheques.postergacion.pdf.dir"));
    }

    @Test
    @DisplayName("CHEQUES_PDF_DIR y POSTERGACIONES_PDF_DIR pisan el defecto (por ejemplo una carpeta compartida montada con -v)")
    void sePuedenPisar() throws IOException {
        Map<String, Object> m = new HashMap<>();
        m.put("uploads.dir", "/app/uploads");
        m.put("CHEQUES_PDF_DIR", "/mnt/compartida/cheques");
        m.put("POSTERGACIONES_PDF_DIR", "/mnt/compartida/postergaciones");
        PropertySourcesPropertyResolver r = resolver(m);

        assertEquals("/mnt/compartida/cheques", r.getProperty("cheques.pdf.dir"));
        assertEquals("/mnt/compartida/postergaciones", r.getProperty("cheques.postergacion.pdf.dir"));
    }

    @Test
    @DisplayName("ningun defecto apunta a una unidad de Windows (D:/Bosque...)")
    void sinRutasDeWindows() throws IOException {
        PropertySourcesPropertyResolver r = resolver(Collections.<String, Object>emptyMap());

        assertFalse(r.getProperty("cheques.pdf.dir").matches("(?i)^[a-z]:.*"), r.getProperty("cheques.pdf.dir"));
        assertFalse(r.getProperty("cheques.postergacion.pdf.dir").matches("(?i)^[a-z]:.*"), r.getProperty("cheques.postergacion.pdf.dir"));
    }
}
