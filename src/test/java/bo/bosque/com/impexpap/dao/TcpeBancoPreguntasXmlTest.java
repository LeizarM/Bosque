package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.TcpeBancoImportarOpcionDto;
import bo.bosque.com.impexpap.dto.TcpeBancoImportarPreguntaDto;
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

/** El XML de {@code @preguntas} de {@code BancoLote} (contrato 9.3): forma exacta, numeración desde 1 y escape. */
class TcpeBancoPreguntasXmlTest {

    private static TcpeBancoImportarOpcionDto opcion(String texto, Boolean valida) {
        TcpeBancoImportarOpcionDto o = new TcpeBancoImportarOpcionDto();
        o.setOpcionRspta(texto);
        o.setEsValido(valida);
        return o;
    }

    private static TcpeBancoImportarPreguntaDto pregunta(String texto, Boolean cerrada, String respuesta,
                                                         Integer prioridad, TcpeBancoImportarOpcionDto... opciones) {
        TcpeBancoImportarPreguntaDto p = new TcpeBancoImportarPreguntaDto();
        p.setPregunta(texto);
        p.setEsCerrada(cerrada);
        p.setRespuesta(respuesta);
        p.setPrioridad(prioridad);
        p.setOpciones(opciones.length == 0 ? null : Arrays.asList(opciones));
        return p;
    }

    private static Document parsear(String xml) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return f.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("forma del contrato: <preguntas><p f c pr><t/><r/><o v/></p></preguntas>")
    void forma() {
        String xml = TcpeBancoDao.armarPreguntasXml(Arrays.asList(
                pregunta(" ¿Capital de Bolivia? ", true, null, 2, opcion("Sucre", true), opcion(" La Paz ", false)),
                pregunta("Explique el ciclo PDCA", false, "Planificar, hacer, verificar, actuar", null)));
        assertEquals("<preguntas>"
                + "<p f=\"1\" c=\"1\" pr=\"2\"><t>¿Capital de Bolivia?</t><r></r>"
                + "<o v=\"1\">Sucre</o><o v=\"0\">La Paz</o></p>"
                + "<p f=\"2\" c=\"0\" pr=\"\"><t>Explique el ciclo PDCA</t><r>Planificar, hacer, verificar, actuar</r></p>"
                + "</preguntas>", xml);
    }

    @Test
    @DisplayName("numera desde 1 en el orden recibido; lo nulo va vacío para que lo marque el SP")
    void numeracionYNulos() {
        String xml = TcpeBancoDao.armarPreguntasXml(Arrays.asList(
                pregunta("A", null, null, null),
                null,
                pregunta("C", true, null, 0, opcion(null, null), null)));
        assertEquals("<preguntas>"
                + "<p f=\"1\" c=\"\" pr=\"\"><t>A</t><r></r></p>"
                + "<p f=\"2\" c=\"\" pr=\"\"><t></t><r></r></p>"
                + "<p f=\"3\" c=\"1\" pr=\"0\"><t>C</t><r></r><o v=\"0\"></o><o v=\"0\"></o></p>"
                + "</preguntas>", xml);
    }

    @Test
    @DisplayName("sin preguntas sale el elemento vacío (el SP responde que no hay preguntas)")
    void vacio() {
        assertEquals("<preguntas></preguntas>", TcpeBancoDao.armarPreguntasXml(null));
        assertEquals("<preguntas></preguntas>", TcpeBancoDao.armarPreguntasXml(Collections.emptyList()));
    }

    @Test
    @DisplayName("escapa lo que escribe el usuario: no abre filas ni opciones y se lee igual")
    void escapaYSeLeeIgual() throws Exception {
        String texto = "a < b && c > \"d\" 'e' </t></p><p f=\"99\" c=\"1\"><t>inyectada";
        String modelo = "R&D <r>";
        String op = "x</o><o v=\"1\">falsa";
        String xml = TcpeBancoDao.armarPreguntasXml(Collections.singletonList(
                pregunta(texto, true, modelo, 1, opcion(op, false))));

        assertFalse(xml.contains("<p f=\"99\""), "el texto del usuario no puede abrir otra fila: " + xml);
        Document d = parsear(xml);
        NodeList ps = d.getElementsByTagName("p");
        assertEquals(1, ps.getLength());
        Element p = (Element) ps.item(0);
        assertEquals("1", p.getAttribute("f"));
        assertEquals(texto, p.getElementsByTagName("t").item(0).getTextContent());
        assertEquals(modelo, p.getElementsByTagName("r").item(0).getTextContent());
        NodeList os = p.getElementsByTagName("o");
        assertEquals(1, os.getLength());
        assertEquals("0", ((Element) os.item(0)).getAttribute("v"));
        assertEquals(op, os.item(0).getTextContent());
    }

    @Test
    @DisplayName("conserva saltos internos, tildes y emojis (el SP decide); descarta controles que XML no admite")
    void caracteresEspeciales() throws Exception {
        String original = "línea 1\nlínea 2\tñandú 😀";
        String xml = TcpeBancoDao.armarPreguntasXml(Collections.singletonList(
                pregunta("\u0001" + original + "\u0007" + (char) 0xFFFF, false, null, null)));

        Element t = (Element) parsear(xml).getElementsByTagName("t").item(0);
        assertEquals(original, t.getTextContent());
    }
}
