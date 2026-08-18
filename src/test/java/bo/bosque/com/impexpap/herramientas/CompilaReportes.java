package bo.bosque.com.impexpap.herramientas;

import net.sf.jasperreports.engine.JasperCompileManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Compila los {@code .jrxml} a {@code .jasper} durante el build.
 *
 * <h3>Que problema resuelve</h3>
 * {@code JasperReportExport.exportPDFStatic} carga el {@code .jasper}
 * precompilado, no el {@code .jrxml}. Hasta ahora esos binarios se generaban a
 * mano en Jaspersoft Studio y se commiteaban, con tres consecuencias:
 *
 * <ol>
 *   <li><b>Un clon limpio no funcionaba.</b> El {@code reports/.gitignore}
 *       excluia 10 de los 46 {@code .jasper} —entre ellos los de la ficha del
 *       trabajador y sus anexos—, asi que produccion andaba solo porque esos
 *       archivos existian sueltos en el disco del servidor. Nadie podia
 *       reconstruir el despliegue desde el repositorio.</li>
 *   <li><b>Riesgo de version.</b> Los {@code .jrxml} los generaron Studios
 *       6.20.6 y 6.21.5, y el runtime es 6.21.2. Un {@code .jasper} compilado
 *       con una version mas nueva puede no cargar. Compilando aca se usa
 *       SIEMPRE la version del pom.</li>
 *   <li><b>Se olvidaba.</b> Editar el {@code .jrxml} sin recompilar deja el
 *       reporte igual que antes, sin ningun aviso. Es un error silencioso que
 *       cuesta una tarde cada vez.</li>
 * </ol>
 *
 * <h3>Vive en src/test a proposito</h3>
 * Es una herramienta de construccion, no codigo de la aplicacion: desde aca no
 * entra al jar. Es lo que le falto a {@code ClassGenerator}, que quedo en
 * {@code src/main} con las credenciales de la base adentro y se empaquetaba en
 * cada despliegue.
 *
 * <h3>Falla el build</h3>
 * Si un {@code .jrxml} no compila, lanza y el build se cae. Antes ese error
 * aparecia como un PDF roto en la cara del usuario.
 *
 * <p>Uso: {@code CompilaReportes <dirFuentes> [dirSalida]}. Sin
 * {@code dirSalida}, escribe junto a cada fuente.
 */
public final class CompilaReportes {

    private CompilaReportes() {
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            throw new IllegalArgumentException("Uso: CompilaReportes <dirFuentes> [dirSalida]");
        }

        File origen = new File(args[0]);
        File[] fuentes = origen.listFiles((d, n) -> n.endsWith(".jrxml"));
        if (fuentes == null) {
            throw new IllegalStateException("No existe el directorio de reportes: " + origen);
        }
        Arrays.sort(fuentes);

        File destino = args.length > 1 ? new File(args[1]) : origen;
        if (!destino.exists() && !destino.mkdirs()) {
            throw new IllegalStateException("No se pudo crear el directorio de salida: " + destino);
        }

        List<String> fallos = new ArrayList<>();
        int ok = 0;

        for (File jrxml : fuentes) {
            File jasper = new File(destino, jrxml.getName().replace(".jrxml", ".jasper"));
            try {
                JasperCompileManager.compileReportToFile(jrxml.getPath(), jasper.getPath());
                ok++;
            } catch (Exception e) {
                fallos.add("  " + jrxml.getName() + "\n      " + resumir(e));
            }
        }

        System.out.println("[reportes] compilados " + ok + "/" + fuentes.length + " -> " + destino);

        if (!fallos.isEmpty()) {
            throw new IllegalStateException("\n\n"
                    + "========================================================================\n"
                    + "  NO COMPILAN " + fallos.size() + " REPORTE(S)\n"
                    + "========================================================================\n\n"
                    + String.join("\n", fallos) + "\n\n"
                    + "  El build se corta a proposito. Un .jrxml que no compila daba antes\n"
                    + "  un PDF roto en produccion, sin aviso previo.\n");
        }
    }

    /** El mensaje de Jasper, recortado y con la causa raiz si la hay. */
    private static String resumir(Throwable t) {
        Throwable raiz = t;
        while (raiz.getCause() != null && raiz.getCause() != raiz) {
            raiz = raiz.getCause();
        }
        String msg = String.valueOf(raiz.getMessage());
        return msg.length() > 400 ? msg.substring(0, 400) + "..." : msg;
    }
}
