package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.TcpeBancoOpcionDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** El XML de {@code @opciones} de {@code p_abm_tcpe_Banco}: forma exacta, escape y que siempre sea XML válido. */
class TcpeBancoOpcionesXmlTest {

    private static TcpeBancoOpcionDto opcion(Long cod, String texto, Boolean valida) {
        TcpeBancoOpcionDto o = new TcpeBancoOpcionDto();
        o.setCodRsptaOp(cod);
        o.setOpcionRspta(texto);
        o.setEsValido(valida);
        return o;
    }

    private static Document parsear(String xml) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return f.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("forma del contrato: <opciones><o c=\"cod o 0\" v=\"1|0\">texto</o></opciones>")
    void forma() {
        String xml = TcpeBancoDao.armarOpcionesXml(Arrays.asList(
                opcion(12L, "Lima", false),
                opcion(null, " La Paz ", true),
                opcion(0L, "Sucre", null)));
        assertEquals("<opciones><o c=\"12\" v=\"0\">Lima</o><o c=\"0\" v=\"1\">La Paz</o>"
                + "<o c=\"0\" v=\"0\">Sucre</o></opciones>", xml);
    }

    @Test
    @DisplayName("sin opciones (abierta) sale el elemento vacío")
    void vacio() {
        assertEquals("<opciones></opciones>", TcpeBancoDao.armarOpcionesXml(null));
        assertEquals("<opciones></opciones>", TcpeBancoDao.armarOpcionesXml(Collections.emptyList()));
    }

    @Test
    @DisplayName("escapa lo que escribe el usuario y el XML resultante se lee igual")
    void escapaYSeLeeIgual() throws Exception {
        String original = "a < b && c > \"d\" 'e' </o><o c=\"99\" v=\"1\">inyectada";
        String xml = TcpeBancoDao.armarOpcionesXml(Collections.singletonList(opcion(5L, original, true)));

        assertFalse(xml.contains("<o c=\"99\""), "el texto del usuario no puede abrir otra opción: " + xml);
        NodeList os = parsear(xml).getElementsByTagName("o");
        assertEquals(1, os.getLength());
        Element o = (Element) os.item(0);
        assertEquals("5", o.getAttribute("c"));
        assertEquals("1", o.getAttribute("v"));
        assertEquals(original, o.getTextContent());
    }

    @Test
    @DisplayName("conserva saltos de línea, tildes y emojis; descarta los controles que XML no admite")
    void caracteresEspeciales() throws Exception {
        String original = "línea 1\nlínea 2\tñandú 😀";
        String conControles = "\u0001" + original + "\u0007￿";
        String xml = TcpeBancoDao.armarOpcionesXml(Collections.singletonList(opcion(1L, conControles, false)));

        Element o = (Element) parsear(xml).getElementsByTagName("o").item(0);
        assertEquals(original, o.getTextContent());
    }
}
