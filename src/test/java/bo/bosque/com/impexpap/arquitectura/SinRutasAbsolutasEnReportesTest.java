package bo.bosque.com.impexpap.arquitectura;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * <b>Un reporte no conoce ninguna ruta del sistema de archivos.</b>
 *
 * Recibe {@code UPLOADS_DIR} y {@code SUBREPORT_DIR} como parametros, y quien
 * decide su valor es Spring segun el perfil: {@code ./uploads} en desarrollo,
 * {@code /app/uploads} dentro del contenedor. Asi el mismo {@code .jrxml} corre
 * en Windows y en Linux sin tocar una linea.
 *
 * <h3>Que paso para que este test exista</h3>
 * Habia <b>26 rutas absolutas</b> repartidas en 15 reportes, de tres epocas
 * distintas del proyecto:
 *
 * <pre>
 *   D:/Proyectos Netbeans/Productiva/web/Bosque/reportes/
 *   D:/Compartido/Bosque v2/web/Bosque/reportes/
 *   D:/proyecto/backend/uploads/
 *   D:/proyecto/FACTURASTIGO/backend/uploads/
 * </pre>
 * (en los archivos venian con contrabarras; aca van con barra porque Java
 * interpreta los escapes unicode ANTES de tokenizar, tambien dentro de los
 * comentarios: una contrabarra seguida de la letra u no compila ni comentada)
 *
 * De esas, <b>11 estaban dentro de expresiones de imagen</b>: en el contenedor
 * esas rutas no existen, asi que el logo de la empresa, la foto del empleado y
 * los anexos (carnet, licencia, pasaporte) <b>salian en blanco en produccion</b>.
 * Y no fallaba: {@code onErrorType="Blank"} dejaba el hueco y nadie se enteraba.
 * En la maquina del que armo el reporte se veia perfecto, porque ahi la ruta si
 * existia. Es la definicion de un bug que solo aparece en el otro entorno.
 *
 * <h3>Por que dos reglas y no una</h3>
 * <ol>
 *   <li><b>Windows y UNC, en cualquier parte del archivo.</b> Un {@code "D:\}
 *       o un {@code "\\servidor\} no tienen ninguna lectura valida dentro de un
 *       reporte. Cero falsos positivos posibles.</li>
 *   <li><b>Rutas absolutas de Unix, SOLO dentro de un
 *       {@code defaultValueExpression}.</b> Tiene que ser mas estrecha: los
 *       reportes concatenan tramos como {@code + "/carnet/" +}, que empiezan
 *       con barra y son legitimos. Lo que no es legitimo es hornear
 *       {@code "/app/uploads/"} como valor por defecto de un parametro: eso ata
 *       el reporte al contenedor y lo rompe en la maquina de cualquiera.</li>
 * </ol>
 *
 * <h3>Como se arregla cuando este test se pone en rojo</h3>
 * Hay una herramienta que lo hace sola y es idempotente:
 * <pre>
 *   mvn test-compile
 *   java -cp target/test-classes \
 *        bo.bosque.com.impexpap.herramientas.CorrigeRutasReportes \
 *        src/main/resources/reports
 * </pre>
 *
 * <h3>No tiene lista de excepciones, y es a proposito</h3>
 * Hoy el conteo esta en <b>cero</b>. Una lista de excepciones vacia es una
 * invitacion a agregarle la primera; sin lista, agregar una ruta absoluta
 * obliga a discutirlo.
 */
class SinRutasAbsolutasEnReportesTest {

    private static final Path REPORTES = Paths.get("src", "main", "resources", "reports");

    /**
     * Unidad de Windows dentro de un literal: {@code "D:} seguido de una o mas
     * contrabarras, o de una barra.
     *
     * <p>Acepta <b>una o dos</b> contrabarras a proposito. En un jrxml las rutas
     * vienen dobladas —el contenido es codigo Java, donde la contrabarra se
     * escapa— pero exigir exactamente dos deja pasar una escrita a mano con una
     * sola. Costaba lo mismo cubrir las dos formas que dejar el hueco.
     */
    private static final Pattern UNIDAD_WINDOWS =
            Pattern.compile("\"[A-Za-z]:[\\\\/]+[^\"]*\"");

    /** Ruta UNC: literal que arranca con contrabarras y nombre de servidor. */
    private static final Pattern RUTA_UNC =
            Pattern.compile("\"[\\\\]{2,}[A-Za-z0-9._-]+[\\\\/][^\"]*\"");

    /** Bloque completo de un valor por defecto de parametro. */
    private static final Pattern DEFAULT_PARAM = Pattern.compile(
            "<defaultValueExpression><!\\[CDATA\\[(.*?)\\]\\]></defaultValueExpression>",
            Pattern.DOTALL);

    /** Literal que arranca en la raiz del sistema de archivos. */
    private static final Pattern RAIZ_UNIX = Pattern.compile("\"/[^\"]*\"");

    @Test
    @DisplayName("ningun reporte lleva una ruta absoluta de Windows o UNC")
    void sinRutasDeWindows() {
        List<String> hallazgos = new ArrayList<>();

        for (Path jrxml : reportes()) {
            String texto = leer(jrxml);
            String[] lineas = texto.split("\r?\n", -1);
            for (int i = 0; i < lineas.length; i++) {
                acumular(hallazgos, jrxml, i + 1, lineas[i], UNIDAD_WINDOWS);
                acumular(hallazgos, jrxml, i + 1, lineas[i], RUTA_UNC);
            }
        }

        if (!hallazgos.isEmpty()) {
            fail(mensaje("RUTAS ABSOLUTAS DE WINDOWS EN LOS REPORTES", hallazgos,
                    "  Esa ruta no existe dentro del contenedor. La imagen o el subreporte\n"
                  + "  no se cargan, y con onErrorType=\"Blank\" no falla nada: simplemente\n"
                  + "  sale el hueco. En la maquina del que edito el reporte se ve bien.\n\n"
                  + "  Usar el parametro en su lugar:\n"
                  + "      $P{UPLOADS_DIR} + \"logos/\" + $F{codEmpresa} + \".png\""));
        }
    }

    @Test
    @DisplayName("ningun parametro tiene una ruta absoluta como valor por defecto")
    void sinRaicesUnixEnLosDefaults() {
        List<String> hallazgos = new ArrayList<>();

        for (Path jrxml : reportes()) {
            String texto = leer(jrxml);
            Matcher bloque = DEFAULT_PARAM.matcher(texto);
            while (bloque.find()) {
                Matcher raiz = RAIZ_UNIX.matcher(bloque.group(1));
                if (raiz.find()) {
                    int linea = 1 + (int) texto.substring(0, bloque.start()).chars()
                            .filter(c -> c == '\n').count();
                    hallazgos.add("  " + jrxml.getFileName() + ":" + linea
                            + "  ->  " + raiz.group());
                }
            }
        }

        if (!hallazgos.isEmpty()) {
            fail(mensaje("RUTAS ABSOLUTAS COMO VALOR POR DEFECTO", hallazgos,
                    "  Un default como \"/app/uploads/\" ata el reporte al contenedor: en\n"
                  + "  Windows esa ruta se resuelve como C:\\app\\uploads y no existe.\n\n"
                  + "  El default tiene que ser relativo (\"uploads/\", \"reports/\") y el\n"
                  + "  valor real lo pone JasperReportExport desde la propiedad uploads.dir."));
        }
    }

    // ==================================================================

    private static List<Path> reportes() {
        try (Stream<Path> s = Files.list(REPORTES)) {
            return s.filter(p -> p.toString().endsWith(".jrxml")).sorted().collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String leer(Path p) {
        try {
            return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void acumular(List<String> destino, Path archivo, int linea,
                                 String contenido, Pattern patron) {
        Matcher m = patron.matcher(contenido);
        while (m.find()) {
            String encontrado = m.group();
            if (encontrado.length() > 90) {
                encontrado = encontrado.substring(0, 90) + "...";
            }
            destino.add("  " + archivo.getFileName() + ":" + linea + "  ->  " + encontrado);
        }
    }

    private static String mensaje(String titulo, List<String> hallazgos, String explicacion) {
        return "\n"
             + "========================================================================\n"
             + "  " + titulo + " (" + hallazgos.size() + ")\n"
             + "========================================================================\n\n"
             + String.join("\n", hallazgos) + "\n\n"
             + explicacion + "\n\n"
             + "  Se arregla solo, y es idempotente:\n"
             + "      mvn test-compile\n"
             + "      java -cp target/test-classes \\\n"
             + "           bo.bosque.com.impexpap.herramientas.CorrigeRutasReportes \\\n"
             + "           src/main/resources/reports\n";
    }
}
