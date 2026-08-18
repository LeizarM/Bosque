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
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * <b>Ningun catch anula el {@code JdbcTemplate}.</b>
 *
 * <h3>El apagon que este test evita</h3>
 * El patron era este, repetido en 73 DAOs:
 *
 * <pre>
 * } catch (BadSqlGrammarException e) {
 *     System.out.println("Error: ...");
 *     this.jdbcTemplate = null;      // &lt;-- aca
 *     resp = 0;
 * }
 * </pre>
 *
 * <p>Los DAOs son {@code @Repository} singleton con inyeccion por campo. Anular
 * el campo no "limpia" nada: <b>destruye el bean para toda la aplicacion</b>.
 * A partir de ese momento cada llamada a ese DAO tira {@code NullPointerException},
 * y sigue asi hasta que alguien reinicie el proceso. Un unico error de sintaxis
 * pasajero —un SP recien alterado, un deploy a medias— apagaba un modulo entero
 * de forma permanente y silenciosa.
 *
 * <p>No es una hipotesis. El codigo mas nuevo ya venia esquivandolo:
 * {@code EntregaChoferDao.sincronizarConSap} arranca preguntando
 * {@code if (this.jdbcTemplate == null)} y {@code AccesoModuloHelper} documenta
 * que el ACL de botones se queda vacio hasta reiniciar cuando le pasa a
 * {@code UsuarioBtnDao}. O sea que ya estaba pasando en produccion y se estaba
 * conviviendo con eso.
 *
 * <h3>Por que un test y no una convencion</h3>
 * Porque eran 154 apariciones. Una regla que hay que recordar no sobrevive a 73
 * archivos y a la proxima persona que copie un DAO viejo para empezar uno nuevo.
 * Un build en rojo si.
 *
 * <h3>Que mira</h3>
 * Solo la <b>asignacion</b>. La comparacion {@code jdbcTemplate == null} es
 * legitima y hay codigo que la usa a proposito para defenderse de este mismo
 * problema; el patron exige el {@code =} sin un segundo {@code =} detras.
 * Tambien ignora comentarios y javadoc, porque varios DAOs explican en prosa por
 * que <em>no</em> lo hacen.
 */
class SinAnularJdbcTemplateTest {

    private static final Path RAIZ = Paths.get("src", "main", "java");

    /** {@code jdbcTemplate = null} pero no {@code jdbcTemplate == null}. */
    private static final Pattern ANULACION =
            Pattern.compile("\\bjdbcTemplate\\s*=\\s*null\\b");

    @Test
    @DisplayName("ningun DAO se anula el JdbcTemplate en un catch")
    void nadieAnulaElJdbcTemplate() {
        List<String> infracciones = new ArrayList<>();

        try (Stream<Path> fuentes = Files.walk(RAIZ)) {
            List<Path> javas = fuentes
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .collect(Collectors.toList());

            for (Path java : javas) {
                List<String> lineas = Files.readAllLines(java, StandardCharsets.UTF_8);
                for (int i = 0; i < lineas.size(); i++) {
                    String linea = sinComentario(lineas.get(i));
                    if (ANULACION.matcher(linea).find()) {
                        infracciones.add(RAIZ.relativize(java) + ":" + (i + 1)
                                + "  ->  " + lineas.get(i).trim());
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        if (!infracciones.isEmpty()) {
            fail("Hay " + infracciones.size() + " asignacion(es) 'jdbcTemplate = null'.\n\n"
               + "Anular el campo de un @Repository singleton deja ese DAO tirando\n"
               + "NullPointerException para TODA la aplicacion hasta el proximo reinicio.\n"
               + "En el catch alcanza con loguear el error y devolver el valor neutro\n"
               + "(false, 0, lista vacia); el JdbcTemplate se deja en paz.\n\n"
               + String.join("\n", infracciones));
        }
    }

    /**
     * Recorta la linea en el primer {@code //} y descarta las de javadoc/bloque.
     *
     * <p>Es a proposito que sea tosco: un {@code //} dentro de un literal de
     * texto haria recortar de mas, y lo unico que puede pasar con eso es que el
     * test deje pasar algo — nunca que falle de mentira. Para el caso que
     * interesa, una asignacion, alcanza y sobra.
     */
    private static String sinComentario(String linea) {
        String limpia = linea.trim();
        if (limpia.startsWith("*") || limpia.startsWith("/*") || limpia.startsWith("//")) {
            return "";
        }
        int barras = limpia.indexOf("//");
        return barras >= 0 ? limpia.substring(0, barras) : limpia;
    }
}
