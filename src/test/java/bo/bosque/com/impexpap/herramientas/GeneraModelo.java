package bo.bosque.com.impexpap.herramientas;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Genera el POJO de {@code model/} a partir de las columnas de una tabla.
 *
 * <p>Es el antiguo {@code utils/ClassGenerator}, con tres cambios.
 *
 * <h3>1. Vive en src/test</h3>
 * Estaba en {@code src/main/java}, asi que Maven lo compilaba y
 * <b>{@code ClassGenerator.class} viajaba dentro del jar de produccion</b> — con
 * la contrasena de {@code sa} en su pool de constantes. Ponerlo en
 * {@code .gitignore} habria sacado el archivo del repositorio pero no del jar:
 * el archivo seguia en disco y el build lo seguia compilando.
 *
 * <p>Desde {@code src/test} la herramienta sigue disponible y no se empaqueta.
 *
 * <h3>2. Sin credenciales escritas</h3>
 * Tenia esto adentro:
 *
 * <pre>
 *   DATABASE_USER = "sa"
 *   DATABASE_PASSWORD = "<CLAVE-ROTADA>"
 * </pre>
 *
 * Ahora salen del entorno, las mismas variables que usa la aplicacion. Sin
 * secreto, el archivo se puede versionar — que es lo que hace falta para que la
 * herramienta sobreviva si esta maquina desaparece.
 *
 * <h3>3. jTDS y parametros por linea de comandos</h3>
 * El driver acompana al del proyecto. Y la clase y la tabla dejan de estar
 * escritas en el {@code main}: se pasan como argumentos.
 *
 * <h3>Uso</h3>
 * <pre>
 *   mvn test-compile
 *
 *   set DB_HOST=192.168.3.116 &amp; set DB_NAME=BOSQUE2PRUEBA
 *   set DB_USERNAME=... &amp; set DB_PASSWORD=...
 *
 *   java -cp "target/test-classes;<i>jtds.jar</i>" \
 *        bo.bosque.com.impexpap.herramientas.GeneraModelo Transacciones tpex_Transacciones
 * </pre>
 *
 * <p>No pisa un archivo existente: si el modelo ya esta, avisa y no hace nada.
 */
public final class GeneraModelo {

    private static final String SALIDA = "src/main/java/bo/bosque/com/impexpap/model/";

    private GeneraModelo() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Uso: GeneraModelo <NombreClase> <nombre_tabla>");
            System.out.println("Variables: DB_HOST, DB_PORT, DB_NAME, DB_USERNAME, DB_PASSWORD");
            return;
        }
        new GeneraModelo().generar(args[0], args[1]);
    }

    /** La URL se arma igual que en application.properties, con jTDS. */
    private static String url() {
        String host = env("DB_HOST", "192.168.3.116");
        String puerto = env("DB_PORT", "1433");
        String base = env("DB_NAME", "BOSQUE2PRUEBA");
        return "jdbc:jtds:sqlserver://" + host + ":" + puerto + "/" + base;
    }

    private static String env(String nombre, String porDefecto) {
        String v = System.getenv(nombre);
        return (v == null || v.trim().isEmpty()) ? porDefecto : v;
    }

    private static String requerida(String nombre) {
        String v = System.getenv(nombre);
        if (v == null || v.trim().isEmpty()) {
            throw new IllegalStateException("Falta la variable de entorno " + nombre);
        }
        return v;
    }

    private void generar(String clase, String tabla) throws Exception {
        String ruta = SALIDA + clase + ".java";
        File destino = new File(ruta);
        if (destino.exists()) {
            System.out.println("La clase ya existe, no se toca: " + destino.getAbsolutePath());
            return;
        }

        List<Columna> columnas = columnas(tabla);
        if (columnas.isEmpty()) {
            System.out.println("Sin columnas para la tabla '" + tabla + "'.");
            System.out.println("Puede no existir, o el usuario no tener permiso para verla.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("package bo.bosque.com.impexpap.model;\n\n");
        sb.append("import java.io.Serializable;\n\n");
        sb.append("import lombok.*;\n\n");
        sb.append("@Getter\n@Setter\n@ToString\n@NoArgsConstructor\n@AllArgsConstructor\n");
        sb.append("public class ").append(clase).append(" implements Serializable {\n");
        for (Columna c : columnas) {
            sb.append("    private ").append(tipoJava(c.tipo))
              .append(' ').append(camelCase(c.nombre)).append(";\n");
        }
        sb.append("}\n");

        escribir(clase + ".java", sb.toString());
        System.out.println("Generada: " + ruta + "  (" + columnas.size() + " columnas)");
    }

    private List<Columna> columnas(String tabla) throws SQLException, ClassNotFoundException {
        List<Columna> columnas = new ArrayList<>();
        Class.forName("net.sourceforge.jtds.jdbc.Driver");
        try (Connection con = DriverManager.getConnection(
                url(), requerida("DB_USERNAME"), requerida("DB_PASSWORD"))) {
            DatabaseMetaData meta = con.getMetaData();
            try (ResultSet rs = meta.getColumns(null, null, tabla, null)) {
                while (rs.next()) {
                    columnas.add(new Columna(rs.getString("COLUMN_NAME"), rs.getString("TYPE_NAME")));
                }
            }
        }
        return columnas;
    }

    private static String camelCase(String entrada) {
        StringBuilder r = new StringBuilder();
        boolean mayuscula = false;
        for (char c : entrada.toCharArray()) {
            if (c == '_') {
                mayuscula = true;
            } else if (mayuscula) {
                r.append(Character.toUpperCase(c));
                mayuscula = false;
            } else {
                r.append(r.length() == 0 ? Character.toLowerCase(c) : c);
            }
        }
        return r.toString();
    }

    private static String tipoJava(String sql) {
        switch (sql.toLowerCase()) {
            case "bigint":
                return "long";
            case "int":
            case "smallint":
            case "tinyint":
                return "int";
            case "bit":
                return "boolean";
            case "decimal":
            case "numeric":
            case "money":
                return "java.math.BigDecimal";
            case "float":
            case "real":
                return "double";
            case "date":
            case "datetime":
            case "datetime2":
            case "smalldatetime":
            case "timestamp":
                return "java.util.Date";
            default:
                return "String";
        }
    }

    private void escribir(String archivo, String contenido) throws IOException {
        File dir = new File(SALIDA);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("No se pudo crear " + SALIDA);
        }
        try (BufferedWriter w = new BufferedWriter(new FileWriter(new File(dir, archivo)))) {
            w.write(contenido);
        }
    }

    /** Reemplaza a utils/Column, que solo existia para esta herramienta. */
    private static final class Columna {
        final String nombre;
        final String tipo;

        Columna(String nombre, String tipo) {
            this.nombre = nombre;
            this.tipo = tipo;
        }
    }
}
