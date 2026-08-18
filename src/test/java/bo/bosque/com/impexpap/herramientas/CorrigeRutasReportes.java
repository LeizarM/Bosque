package bo.bosque.com.impexpap.herramientas;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Saca las rutas absolutas de Windows de los .jrxml.
 *
 * Dos transformaciones distintas, por motivos distintos:
 *
 *  A) Dentro de imageExpression: son BUGS ACTIVOS. En el contenedor esas rutas
 *     no existen y la imagen sale en blanco sin avisar. Se reemplaza el tramo
 *     hasta "uploads" por el parametro UPLOADS_DIR, que Spring resuelve por
 *     perfil (./uploads en dev, /app/uploads en prod).
 *
 *  B) En el defaultValueExpression de SUBREPORT_DIR: son inofensivas, porque
 *     JasperReportExport siempre pasa ese parametro y el default nunca se usa.
 *     Pero son fosiles de tres epocas del proyecto (Bosque v2, Proyectos
 *     Netbeans, D:/proyecto/backend). Se normalizan a "reports/" para que el
 *     test de arquitectura pueda exigir CERO rutas absolutas, sin lista de
 *     excepciones que despues nadie mantiene.
 *
 * Idempotente: correrlo dos veces no cambia nada la segunda vez.
 */
public class CorrigeRutasReportes {

    /** Literal que pasa por un directorio "uploads", con barras o contrabarras. */
    private static final Pattern RUTA_UPLOADS = Pattern.compile(
            "\"[A-Za-z]:(?:\\\\\\\\|/)[^\"]*?uploads(?:\\\\\\\\|/)([^\"]*)\"");

    /** Cualquier literal que empiece con una unidad de Windows. */
    private static final Pattern RUTA_ABSOLUTA =
            Pattern.compile("\"[A-Za-z]:(?:\\\\\\\\|/)[^\"]*\"");

    /**
     * Bloques de expresion que pueden llevar una ruta de archivo.
     *
     * printWhenExpression va incluido a proposito: en subRptAnexos las rutas
     * aparecen DOS veces por documento — una en la imagen y otra en el
     * printWhen que decide si el bloque se muestra. Arreglar solo la imagen no
     * sirve de nada: el File(...).exists() del printWhen sigue dando false y la
     * seccion entera no se imprime.
     *
     * La retro-referencia \1 obliga a que el tag de cierre sea el mismo que el
     * de apertura.
     */
    private static final Pattern BLOQUE_IMAGEN = Pattern.compile(
            "<(imageExpression|printWhenExpression)>.*?</\\1>", Pattern.DOTALL);

    /** Literal que arranca en la raiz del sistema de archivos. */
    private static final Pattern RAIZ_UNIX = Pattern.compile("\"/[^\"]*\"");

    /**
     * Valor por defecto de CUALQUIER parametro; el grupo 2 es el nombre, y es el
     * que decide con que se reemplaza.
     *
     * <p>Antes esto miraba solo SUBREPORT_DIR, y se le escapaban los
     * {@code "/app/uploads/"} que traian los reportes ya "arreglados" en el
     * servidor. Cambiar la ruta de Windows por la del contenedor es el mismo
     * problema visto desde el otro sistema operativo: en una maquina Windows,
     * {@code /app/uploads} se resuelve contra la unidad actual y no existe.
     *
     * <p>El default correcto es RELATIVO. El valor real lo pone
     * JasperReportExport desde la propiedad {@code uploads.dir}, que cambia por
     * perfil.
     */
    private static final Pattern DEFAULT_SUBREPORT = Pattern.compile(
            "(<parameter\\s+name=\"([A-Za-z_]+)\"[^>]*>\\s*"
          + "<defaultValueExpression><!\\[CDATA\\[)(\"[^\"]*\")"
          + "(\\]\\]></defaultValueExpression>)", Pattern.DOTALL);

    private static final String DECL_UPLOADS =
            "\t<parameter name=\"UPLOADS_DIR\" class=\"java.lang.String\" isForPrompting=\"false\">\n"
          + "\t\t<defaultValueExpression><![CDATA[\"uploads/\"]]></defaultValueExpression>\n"
          + "\t</parameter>\n";

    public static void main(String[] args) throws IOException {
        Path dir = Paths.get(args[0]);
        boolean simular = args.length > 1 && "--simular".equals(args[1]);
        int tocados = 0, imagenes = 0, defaults = 0, declarados = 0;

        List<Path> fuentes;
        try (Stream<Path> s = Files.list(dir)) {
            fuentes = s.filter(p -> p.toString().endsWith(".jrxml")).sorted().collect(Collectors.toList());
        }

        for (Path f : fuentes) {
            String original = new String(Files.readAllBytes(f), StandardCharsets.UTF_8);
            String texto = original;
            int imgAqui = 0, defAqui = 0;

            // ---- A) rutas dentro de imageExpression ----
            Matcher bloque = BLOQUE_IMAGEN.matcher(texto);
            StringBuffer sb = new StringBuffer();
            while (bloque.find()) {
                Matcher ruta = RUTA_UPLOADS.matcher(bloque.group());
                StringBuffer nuevo = new StringBuffer();
                while (ruta.find()) {
                    String resto = ruta.group(1).replace("\\\\", "/");
                    String reemplazo = resto.isEmpty()
                            ? "$P{UPLOADS_DIR}"
                            : "$P{UPLOADS_DIR} + \"" + resto + "\"";
                    ruta.appendReplacement(nuevo, Matcher.quoteReplacement(reemplazo));
                    imgAqui++;
                }
                ruta.appendTail(nuevo);

                // Los separadores que quedan DENTRO de la ruta tambien eran
                // contrabarras: "documentos/" + cod + "\\carnet\\" + ...
                // En Linux la contrabarra no separa nada, pasa a ser parte del
                // nombre del archivo, y el resultado es un fichero llamado
                // literalmente "172\carnet\172_carnet_anverso.jpg" que no
                // existe. Se normalizan a barra, que funciona en los dos
                // sistemas: Java acepta '/' tambien en Windows.
                String expr = nuevo.toString();
                if (expr.contains("$P{UPLOADS_DIR}")) {
                    expr = expr.replace("\\\\", "/");
                }
                bloque.appendReplacement(sb, Matcher.quoteReplacement(expr));
            }
            bloque.appendTail(sb);
            texto = sb.toString();

            // ---- Declarar UPLOADS_DIR si ahora se usa y no estaba ----
            if (texto.contains("$P{UPLOADS_DIR}") && !texto.contains("name=\"UPLOADS_DIR\"")) {
                int corte = texto.lastIndexOf("</parameter>");
                if (corte >= 0) {
                    corte += "</parameter>".length() + 1;
                } else {
                    corte = texto.indexOf("<queryString");
                }
                if (corte > 0) {
                    texto = texto.substring(0, corte) + DECL_UPLOADS + texto.substring(corte);
                    declarados++;
                }
            }

            // ---- B) valores por defecto con ruta absoluta ----
            Matcher d = DEFAULT_SUBREPORT.matcher(texto);
            StringBuffer sd = new StringBuffer();
            while (d.find()) {
                String nombre = d.group(2);
                String literal = d.group(3);
                boolean absoluta = RUTA_ABSOLUTA.matcher(literal).matches()
                                || RAIZ_UNIX.matcher(literal).matches();
                if (absoluta) {
                    // Los subreportes viven en el classpath, bajo reports/.
                    // Cualquier otro parametro de ruta apunta a los archivos
                    // subidos, y su raiz la decide uploads.dir por perfil.
                    String relativo = "SUBREPORT_DIR".equals(nombre) ? "\"reports/\"" : "\"uploads/\"";
                    d.appendReplacement(sd,
                            Matcher.quoteReplacement(d.group(1) + relativo + d.group(4)));
                    defAqui++;
                } else {
                    d.appendReplacement(sd, Matcher.quoteReplacement(d.group()));
                }
            }
            d.appendTail(sd);
            texto = sd.toString();

            imagenes += imgAqui;
            defaults += defAqui;

            if (!texto.equals(original)) {
                tocados++;
                System.out.printf("%-32s  imagenes:%d  subreportDir:%d%n",
                        f.getFileName(), imgAqui, defAqui);
                if (!simular) {
                    Files.write(f, texto.getBytes(StandardCharsets.UTF_8));
                }
            }
        }

        System.out.println();
        System.out.printf("archivos tocados: %d | rutas de imagen: %d | defaults SUBREPORT_DIR: %d | UPLOADS_DIR declarado: %d%n",
                tocados, imagenes, defaults, declarados);
        if (simular) {
            System.out.println("(SIMULACION: no se escribio nada)");
        }
    }
}
