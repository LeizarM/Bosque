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
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * <b>En este proyecto Java no arma SQL.</b> Toda la lógica de datos vive en procedimientos
 * almacenados; el código sólo los invoca.
 *
 * <h3>Por qué existe este test</h3>
 * La regla estaba escrita en la cabeza de la gente y en ningún lado más, así que se rompió: el
 * módulo de permisos llegó a tener ocho consultas armadas en Java, cada una con un comentario que
 * la declaraba «excepción consciente» y un {@code TODO: pedir al DBA}. Ocho excepciones conscientes
 * no son excepciones, son la regla incumplida. Un comentario no frena a nadie; un build en rojo sí.
 *
 * <h3>Qué mira, y por qué así</h3>
 * Un {@code grep} de {@code "SELECT"} es fácil de evadir sin querer: el SQL se parte entre líneas
 * ({@code "SELECT a" + " FROM b"}) y entonces ninguna línea sola parece una consulta. Por eso acá
 * se <b>pegan los literales contiguos</b> antes de mirarlos, y se ignoran comentarios y javadoc con
 * un recorrido carácter por carácter en vez de una expresión regular.
 *
 * <h3>Lo que NO puede ver</h3>
 * SQL armado en ejecución con {@code StringBuilder} a partir de pedazos que por separado no
 * parecen SQL. No hay forma barata de atrapar eso leyendo el fuente. Si algún día hace falta,
 * la respuesta es envolver el {@code JdbcTemplate} y rechazar en ejecución lo que no empiece con
 * {@code EXEC} — pero eso falla tarde, en producción, y por eso no es lo primero.
 *
 * <h3>Cómo agregar una excepción</h3>
 * Sumar una entrada a {@link #PERMITIDOS} <b>con su motivo escrito</b>. Que cueste una línea de
 * texto es a propósito: obliga a decir por qué, y deja el porqué al lado del código en vez de en
 * un chat. La excepción queda atada al fragmento exacto, así que SQL nuevo en el mismo archivo
 * vuelve a romper el build.
 */
class SinSqlCrudoTest {

    private static final Path RAIZ = Paths.get("src", "main", "java");

    /**
     * Señales FUERTES: cada una alcanza sola. Son formas que no aparecen en prosa.
     *
     * <p>Deliberadamente específicas. {@code UPDATE} suelto vive en mensajes al usuario, así que se
     * exige {@code UPDATE <algo> SET}. Un test que grita en falso se termina ignorando, y un test
     * ignorado es peor que no tenerlo.
     */
    private static final List<Pattern> FUERTES = Arrays.asList(
            Pattern.compile("\\bSELECT\\b[\\s\\S]*?\\bFROM\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bINSERT\\s+INTO\\b",             Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bUPDATE\\s+[\\w\\[\\].]+\\s+SET\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bDELETE\\s+FROM\\b",             Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bMERGE\\s+INTO\\b",              Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bTRUNCATE\\s+TABLE\\b",          Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(CREATE|ALTER|DROP)\\s+(TABLE|VIEW|PROCEDURE|FUNCTION|INDEX)\\b",
                            Pattern.CASE_INSENSITIVE)
    );

    /**
     * Señales DÉBILES: hacen falta dos para marcar.
     *
     * <p>Existen para el SQL partido en pedazos donde el {@code SELECT} quedó en otro literal que
     * no se pudo pegar. Sueltas no sirven: la primera versión de este test marcaba
     * <i>«Blocked authentication attempt from IP»</i> porque {@code from} es una palabra del
     * inglés. Exigir dos es lo que separa una consulta de una frase.
     */
    private static final List<Pattern> DEBILES = Arrays.asList(
            Pattern.compile("\\bFROM\\s+(dbo\\.)?[\\w\\[]",     Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(INNER|LEFT|RIGHT|FULL|CROSS)\\s+(JOIN|APPLY)\\b",
                            Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bOUTER\\s+APPLY\\b",             Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(GROUP|ORDER)\\s+BY\\b",        Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bWHERE\\s+[\\w\\[]",             Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bISNULL\\s*\\(|\\bCOALESCE\\s*\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bCONVERT\\s*\\(\\s*(date|datetime|varchar|int)\\b",
                            Pattern.CASE_INSENSITIVE)
    );

    /** Si el trozo es SQL según las reglas de arriba. */
    private static boolean pareceSql(String texto) {
        if (FUERTES.stream().anyMatch(s -> s.matcher(texto).find())) return true;
        return DEBILES.stream().filter(s -> s.matcher(texto).find()).count() >= 2;
    }

    /**
     * Una llamada a un SP, que es justamente lo permitido. Si el literal la tiene, no se mira más:
     * {@code "execute p_abm_Permiso @codPermiso=?, ..."} no es SQL crudo aunque diga {@code SET}.
     */
    private static final Pattern LLAMA_A_SP = Pattern.compile(
            "\\b(EXEC|EXECUTE)\\s+(dbo\\.)?[\\w\\[]", Pattern.CASE_INSENSITIVE);

    /** Una excepción con su motivo. El motivo no es decoración: es el precio de la excepción. */
    private static final class Permitido {
        final String archivo;    // sufijo de la ruta
        final String fragmento;  // texto que el literal DEBE contener para quedar exento
        final String motivo;

        Permitido(String archivo, String fragmento, String motivo) {
            this.archivo = archivo;
            this.fragmento = fragmento;
            this.motivo = motivo;
        }
    }

    private static final List<Permitido> PERMITIDOS = Arrays.asList(
            new Permitido(
                    "utils/SpHelper.java", "@__error AS [error]",
                    "Es la maquinaria que llama a los SP: lee los OUTPUT del propio SP que acaba "
                  + "de ejecutar. Meterlo en un SP sería circular."),
            new Permitido(
                    "dao/EntregaChoferDao.java", "OBJECT_ID('p_abm_trch_EntregasSync')",
                    "Pregunta si un SP existe, para decidir entre el camino nuevo y el histórico. "
                  + "Preguntar por un SP desde un SP sería circular."),
            new Permitido(
                    // El fragmento se ancla en la PRIMERA mitad de la consulta a propósito: el
                    // literal se corta en "FROM [" porque ahí se interpola linkedServer, así que
                    // "dbo.OCRD" cae en otro trozo y no serviría como ancla.
                    "commons/ContactoClienteSapResolver.java", "ISNULL(o.Cellular",
                    "Consulta a SAP por linked server, con el nombre de la base tomado de "
                  + "configuración. Como SP necesitaría SQL dinámico adentro, que es peor."),
            // Aquí vivía la excepción de security/SecurityFilter.java, por la expresión
            // regular "union|select|from|where" con la que ese filtro pretendía detectar
            // inyección SQL. Se borró junto con la regex: rechazaba cualquier parámetro
            // con una comilla o con la palabra "from", y no defendía de nada, porque en
            // este proyecto no se concatena SQL — que es justamente lo que garantiza este
            // test. Ver el javadoc de SecurityFilter.

            // ── Deuda conocida ───────────────────────────────────────────────────────────
            // Las dos de abajo SÍ son violaciones. Están acá para que el test entre en verde
            // hoy y no para bendecirlas: los dos SP ya existen, sólo falta agregarles una
            // ACCION. Cuando se haga, se borran estas dos entradas.
            new Permitido(
                    "dao/GaranteReferenciaDao.java", "FROM trh_garanteReferencia",
                    "DEUDA: dos SELECT COUNT(*). Van como ACCION de p_list_GaranteReferencia."),
            new Permitido(
                    "dao/ProcesoRolDao.java", "FROM dbo.trs_Rol",
                    "DEUDA: relee el idRol recién generado. Va como ACCION de p_list_trs_Rol.")
    );

    @Test
    @DisplayName("ningún archivo de src/main/java arma SQL: todo pasa por un procedimiento almacenado")
    void nadieArmaSql() throws IOException {
        List<String> hallazgos = new ArrayList<>();

        try (Stream<Path> archivos = Files.walk(RAIZ)) {
            archivos.filter(p -> p.toString().endsWith(".java"))
                    .sorted()
                    .forEach(p -> revisar(p, hallazgos));
        }

        if (hallazgos.isEmpty()) return;

        // Sin acentos ni caracteres de caja A PROPOSITO. Este mensaje se lee en la consola
        // justo cuando el build se rompe, y la consola de Windows es cp1252: los acentos y
        // los bordes lindos salen como '?'. Es el unico texto del proyecto con esta regla,
        // porque es el unico cuyo trabajo es ser legible en una terminal cualquiera.
        fail(String.join("\n", Arrays.asList(
                "",
                "========================================================================",
                "  HAY SQL ARMADO EN JAVA. En este proyecto eso no va.",
                "========================================================================",
                "",
                String.join("\n\n", hallazgos),
                "",
                "------------------------------------------------------------------------",
                "  QUE HACER",
                "------------------------------------------------------------------------",
                "  1. Agrega una ACCION al procedimiento que corresponda y llamala con",
                "     SpHelper:",
                "",
                "         Map<String, Object> filtro = new HashMap<>();",
                "         filtro.put(\"codEmpleado\", codEmpleado);",
                "         spHelper.ejecutarListado(\"p_list_Xxx\", filtro, \"A1\", Dto.class);",
                "",
                "     Para columnas dinamicas: spHelper.ejecutarListadoDinamico(sp, filtro),",
                "     con el ACCION adentro del mapa.",
                "",
                "  2. El script del ALTER va en sql/, numerado, y se corre en BOSQUE2PRUEBA",
                "     y en BOSQUE-2_0 ANTES de desplegar el jar.",
                "",
                "  3. Si de verdad no puede ser un SP (consulta a linked server, preguntar",
                "     si un SP existe), suma una entrada a PERMITIDOS en",
                "     " + nombreDeEsteTest(),
                "     CON EL MOTIVO ESCRITO. Un TODO en un comentario no alcanza: asi fue",
                "     como el modulo de permisos junto ocho de estas.",
                ""
        )));
    }

    private static void revisar(Path archivo, List<String> hallazgos) {
        final String fuente;
        try {
            fuente = new String(Files.readAllBytes(archivo), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        for (Trozo t : literalesPegados(fuente)) {
            if (LLAMA_A_SP.matcher(t.texto).find()) continue;
            if (!pareceSql(t.texto)) continue;
            if (permitido(archivo, t.texto)) continue;

            hallazgos.add("  " + archivo.toString().replace('\\', '/') + ":" + t.linea + "\n"
                        + "      " + recortar(t.texto));
        }
    }

    private static boolean permitido(Path archivo, String texto) {
        String ruta = archivo.toString().replace('\\', '/');
        return PERMITIDOS.stream().anyMatch(
                p -> ruta.endsWith(p.archivo) && texto.contains(p.fragmento));
    }

    private static String recortar(String texto) {
        String plano = texto.replaceAll("\\s+", " ").trim();
        return plano.length() <= 120 ? plano : plano.substring(0, 117) + "...";
    }

    private static String nombreDeEsteTest() {
        return "src/test/java/bo/bosque/com/impexpap/arquitectura/SinSqlCrudoTest.java";
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // El escáner
    // ══════════════════════════════════════════════════════════════════════════════

    /**
     * Un literal, con dónde empieza y termina EN EL FUENTE.
     *
     * <p>Los offsets son del fuente, no del texto ya procesado. La primera versión los dedujo
     * restando el largo del texto — y falló, porque las secuencias de escape hacen que el texto
     * sea más corto que su fuente. Ahí el pegado dejó de pegar y {@code "SELECT a" + " FROM b"}
     * pasó de largo, que es justamente el caso que este test existe para atrapar.
     */
    private static final class Trozo {
        final String texto;
        final int linea;
        final int inicio;
        final int fin;

        Trozo(String texto, int linea, int inicio, int fin) {
            this.texto = texto;
            this.linea = linea;
            this.inicio = inicio;
            this.fin = fin;
        }
    }

    /**
     * Los literales del archivo, con los contiguos pegados.
     *
     * <p>«Contiguos» quiere decir separados sólo por espacios y {@code +}, que es exactamente cómo
     * queda una consulta partida en varias líneas. Sin pegarlos, {@code "SELECT a"} y
     * {@code " FROM b"} pasan sueltos y ninguno de los dos parece gran cosa.
     *
     * <p>Recorre carácter por carácter en vez de usar una expresión regular porque hay que
     * distinguir un {@code "} dentro de un comentario de uno que abre un literal, y un {@code //}
     * dentro de un literal (una URL) de uno que abre un comentario. Una regex no sabe en cuál de
     * los dos mundos está parada.
     */
    private static List<Trozo> literalesPegados(String fuente) {
        List<Trozo> sueltos = new ArrayList<>();

        int i = 0, linea = 1;
        final int n = fuente.length();

        while (i < n) {
            char c = fuente.charAt(i);

            if (c == '\n') { linea++; i++; continue; }

            // Comentario de línea
            if (c == '/' && i + 1 < n && fuente.charAt(i + 1) == '/') {
                while (i < n && fuente.charAt(i) != '\n') i++;
                continue;
            }
            // Comentario de bloque (incluye javadoc)
            if (c == '/' && i + 1 < n && fuente.charAt(i + 1) == '*') {
                i += 2;
                while (i + 1 < n && !(fuente.charAt(i) == '*' && fuente.charAt(i + 1) == '/')) {
                    if (fuente.charAt(i) == '\n') linea++;
                    i++;
                }
                i = Math.min(i + 2, n);
                continue;
            }
            // Literal de carácter: 'x', '\'', '\\'
            if (c == '\'') {
                i++;
                while (i < n && fuente.charAt(i) != '\'') {
                    if (fuente.charAt(i) == '\\') i++;
                    i++;
                }
                i++;
                continue;
            }
            // Literal de texto
            if (c == '"') {
                final int inicio = i;
                final int arranca = linea;
                StringBuilder sb = new StringBuilder();
                i++;
                while (i < n && fuente.charAt(i) != '"') {
                    char d = fuente.charAt(i);
                    if (d == '\\' && i + 1 < n) {
                        char e = fuente.charAt(i + 1);
                        // Sólo interesan las secuencias que cambian el texto de la consulta.
                        if (e == 'n' || e == 't' || e == 'r') sb.append(' ');
                        else sb.append(e);
                        i += 2;
                        continue;
                    }
                    if (d == '\n') linea++;   // no debería pasar en Java 8, pero no cuesta
                    sb.append(d);
                    i++;
                }
                i++;                                   // se come la comilla de cierre
                sueltos.add(new Trozo(sb.toString(), arranca, inicio, i));
                continue;
            }
            i++;
        }

        // Pegar los que están separados sólo por espacios y un '+'.
        List<Trozo> pegados = new ArrayList<>();
        int k = 0;
        while (k < sueltos.size()) {
            Trozo primero = sueltos.get(k);
            StringBuilder junto = new StringBuilder(primero.texto);
            int fin = primero.fin;

            int j = k + 1;
            while (j < sueltos.size()) {
                Trozo siguiente = sueltos.get(j);
                String medio = fuente.substring(fin, siguiente.inicio);
                // Entre los dos sólo puede haber espacios y exactamente un '+'.
                if (!medio.matches("\\s*\\+\\s*")) break;
                junto.append(siguiente.texto);
                fin = siguiente.fin;
                j++;
            }

            pegados.add(new Trozo(junto.toString(), primero.linea, primero.inicio, fin));
            k = j;
        }
        return pegados;
    }

    /**
     * Ninguna excepción sobra.
     *
     * <p>Una lista blanca se pudre así: alguien arregla la violación, se olvida de borrar la
     * entrada, y la entrada queda tapando código que todavía no existe. Cuando de verdad se toque
     * ese archivo, el guardarraíl mira para otro lado. Por eso una excepción que ya no aplica es
     * un error igual que una violación.
     *
     * <p>Vale sobre todo para las dos marcadas {@code DEUDA}: el día que {@code GaranteReferenciaDao}
     * y {@code ProcesoRolDao} pasen a usar sus SP, este test avisa que hay que borrarlas.
     */
    @Test
    @DisplayName("no quedan excepciones de más: cada entrada de PERMITIDOS todavía tapa algo")
    void ningunaExcepcionSobra() throws IOException {
        List<String> sobrantes = new ArrayList<>();

        for (Permitido p : PERMITIDOS) {
            boolean usada;
            try (Stream<Path> archivos = Files.walk(RAIZ)) {
                usada = archivos
                        .filter(a -> a.toString().replace('\\', '/').endsWith(p.archivo))
                        .anyMatch(a -> {
                            try {
                                String fuente = new String(Files.readAllBytes(a),
                                                           StandardCharsets.UTF_8);
                                return literalesPegados(fuente).stream()
                                        .anyMatch(t -> t.texto.contains(p.fragmento));
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        });
            }
            if (!usada) {
                sobrantes.add("  " + p.archivo + "  ->  no se encontró \"" + p.fragmento + "\"\n"
                            + "      motivo que decía: " + p.motivo);
            }
        }

        if (sobrantes.isEmpty()) return;

        fail(String.join("\n", Arrays.asList(
                "",
                "========================================================================",
                "  HAY EXCEPCIONES DE MAS EN PERMITIDOS. Borralas.",
                "========================================================================",
                "",
                String.join("\n\n", sobrantes),
                "",
                "  El SQL que tapaban ya no esta (o se movio). Una excepcion que no tapa",
                "  nada queda esperando a cubrir codigo futuro sin que nadie se entere.",
                "  Si arreglaste una DEUDA: borra su entrada, ese es el ultimo paso.",
                ""
        )));
    }

    /** Que el escáner no se coma un archivo entero por un carácter raro. */
    @Test
    @DisplayName("el escáner sabe distinguir un literal de un comentario")
    void elEscanerNoSeConfunde() {
        String fuente = String.join("\n", Arrays.asList(
                "class X {",
                "  // SELECT * FROM esto_es_un_comentario",
                "  /* SELECT * FROM esto_tambien */",
                "  String url = \"https://ejemplo/no/es/comentario\";",
                "  char comilla = '\"';",
                "  String sp = \"execute p_abm_Permiso @codPermiso=?, @ACCION=?\";",
                "  String partida = \"SELECT a\"",
                "                 + \"  FROM b\";",
                "}"));

        List<Trozo> t = literalesPegados(fuente);
        List<String> textos = t.stream().map(x -> x.texto).collect(Collectors.toList());

        // El comentario no aparece por ningún lado.
        if (textos.stream().anyMatch(x -> x.contains("esto_es_un_comentario")
                                       || x.contains("esto_tambien"))) {
            fail("el escáner tomó un comentario como literal: " + textos);
        }
        // La URL sí, y su '//' no abrió un comentario.
        if (textos.stream().noneMatch(x -> x.contains("https://ejemplo"))) {
            fail("el escáner perdió la URL: " + textos);
        }
        // La consulta partida quedó pegada, que es el caso que un grep no ve.
        if (textos.stream().noneMatch(x -> x.contains("SELECT a  FROM b"))) {
            fail("el escáner no pegó los literales contiguos: " + textos);
        }
        // El EXEC no se marca; la consulta partida sí.
        if (!LLAMA_A_SP.matcher("execute p_abm_Permiso @codPermiso=?").find()) {
            fail("una llamada a SP no se reconoció como tal");
        }
        if (!pareceSql("SELECT a  FROM b")) {
            fail("una consulta de verdad NO quedó marcada");
        }
        // Prosa en inglés: 'from' sola no alcanza. Este era el falso positivo.
        if (pareceSql("Blocked authentication attempt from IP: {}")) {
            fail("una frase en inglés quedó marcada como SQL");
        }
        if (pareceSql("Possible attack pattern detected from IP {}: {}")) {
            fail("una frase en inglés quedó marcada como SQL");
        }
    }
}
