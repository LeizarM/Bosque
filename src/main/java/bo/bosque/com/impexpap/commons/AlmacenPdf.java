package bo.bosque.com.impexpap.commons;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.LongFunction;
import java.util.function.Supplier;

import org.slf4j.Logger;

import bo.bosque.com.impexpap.config.SpBusinessException;

/**
 * La parte <b>tecnica</b> comun de los documentos PDF del modulo de cheques (el del cheque, {@link ChequePdfService}, y el de la
 * postergacion, {@link PostergacionPdfService}): carpeta del servidor, lectura de la cabecera, escritura atomica y fecha de
 * modificacion. No decide ninguna regla de negocio ni redacta ningun mensaje: cada servicio pasa los suyos (cada documento
 * tiene su propia carpeta y sus propios textos).
 *
 * <p>Las reglas que comparten ambos documentos (las del {@code p:fileUpload} del legacy) tambien viven aqui como constantes:
 * menos de 2 MegaBytes ({@link #TAMANO_MAXIMO_BYTES}) y marca {@code %PDF-} en los primeros {@link #VENTANA_DE_LA_MARCA} bytes.
 *
 * <p>La carpeta vive dentro de {@code uploads.dir} (ruta relativa a la aplicacion, como Depositos y Pagos al exterior): se
 * <b>crea al subir el primer PDF</b>, y mientras no exista ningun documento esta guardado (leer una carpeta que aun no existe
 * equivale a no tener PDF). Lo que si es un error es una ruta que no es una carpeta o a la que no se puede leer o escribir.
 */
final class AlmacenPdf {

    private AlmacenPdf() {
    }

    /** {@code sizeLimit="2010000"} del {@code p:fileUpload} del legacy, en bytes. */
    static final long TAMANO_MAXIMO_BYTES = 2_010_000L;

    /** Los primeros bytes donde tiene que aparecer la marca de un PDF (la especificacion tolera basura antes de ella). */
    static final int VENTANA_DE_LA_MARCA = 1024;

    private static final String MARCA_PDF = "%PDF-";

    /**
     * La zona del proyecto ({@code spring.jackson.time-zone}, {@code JacksonConfig}). La fecha de modificacion sale ya como
     * texto, asi que Jackson no la convierte: con la zona por defecto de la JVM, el contenedor (que corre en UTC) la mostraria
     * 4 horas adelantada.
     */
    static final ZoneId ZONA = ZoneId.of("America/La_Paz");

    static final Clock RELOJ = Clock.system(ZONA);

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * La carpeta de los PDF. Para <b>subir</b> ({@code escribir = true}) la crea si no existe; para <b>leer</b> devuelve la ruta
     * aunque la carpeta aun no exista (el que llama comprueba cada archivo, y un archivo que no esta es «sin PDF»).
     *
     * @param ruta          el valor configurado (puede ser null o vacio)
     * @param escribir      true para subir (ademas de leer hay que poder escribir; si no existe, se crea)
     * @param noConfigurada el error cuando la ruta esta vacia
     * @param noDisponible  el error cuando la ruta no sirve; recibe la ruta y el motivo ("no se pudo crear", "no es una carpeta",
     *                      "no se puede leer", "no se puede escribir en ella", "la ruta no es válida")
     */
    static Path carpetaDisponible(String ruta, boolean escribir, Supplier<SpBusinessException> noConfigurada,
                                  BiFunction<String, String, SpBusinessException> noDisponible, Logger log) {
        String r = ruta == null ? "" : ruta.trim();
        if (r.isEmpty()) throw noConfigurada.get();

        Path dir;
        try {
            dir = Paths.get(r);
        } catch (InvalidPathException e) {
            throw sinCarpeta(r, "la ruta no es válida", noDisponible, log);
        }
        if (!Files.exists(dir)) {
            if (!escribir) return dir;   // aun no se subio ningun PDF: no hay nada que leer
            try {
                Files.createDirectories(dir);
                log.info("Carpeta de PDF creada: {}", dir.toAbsolutePath());
            } catch (IOException | RuntimeException e) {
                log.warn("No se pudo crear la carpeta de PDF {}: {}", r, e.toString());
                throw sinCarpeta(r, "no se pudo crear", noDisponible, log);
            }
        }
        if (!Files.isDirectory(dir)) throw sinCarpeta(r, "no es una carpeta", noDisponible, log);
        if (!Files.isReadable(dir)) throw sinCarpeta(r, "no se puede leer", noDisponible, log);
        if (escribir && !Files.isWritable(dir)) throw sinCarpeta(r, "no se puede escribir en ella", noDisponible, log);
        return dir;
    }

    private static SpBusinessException sinCarpeta(String ruta, String motivo,
                                                  BiFunction<String, String, SpBusinessException> noDisponible, Logger log) {
        log.warn("Carpeta de PDF no disponible ({}): {}", ruta, motivo);
        return noDisponible.apply(ruta, motivo);
    }

    /** Si la cabecera leida trae la marca de un PDF. */
    static boolean tieneMarcaPdf(byte[] cabecera) {
        return new String(cabecera, StandardCharsets.ISO_8859_1).contains(MARCA_PDF);
    }

    /** Hasta {@code n} bytes del principio (menos si el archivo es mas corto). */
    static byte[] leerHasta(InputStream in, int n) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(Math.min(n, 8192));
        byte[] buffer = new byte[Math.min(n, 8192)];
        int faltan = n;
        while (faltan > 0) {
            int leidos = in.read(buffer, 0, Math.min(buffer.length, faltan));
            if (leidos < 0) break;
            bytes.write(buffer, 0, leidos);
            faltan -= leidos;
        }
        return bytes.toByteArray();
    }

    /**
     * Escribe {@code cabecera} y el resto de {@code entrada} en un temporal de la misma carpeta y lo mueve sobre {@code destino}.
     * El temporal se borra si algo falla (incluido el movimiento). Corta si el peso real pasa el limite aunque el servidor
     * hubiera declarado menos.
     *
     * @param prefijoTemporal p. ej. {@code "cheque-18129"}: el temporal se llama {@code <prefijo>-<uuid>.tmp}
     * @param muyGrande       el error cuando el peso real pasa el limite; recibe los bytes leidos hasta ese momento
     * @return los bytes escritos
     */
    static long guardarAtomico(Path dir, Path destino, String prefijoTemporal, byte[] cabecera, InputStream entrada,
                               LongFunction<SpBusinessException> muyGrande, Logger log) throws IOException {
        // Nombre propio y CREATE_NEW, no Files.createTempFile: este ultimo crea el archivo con permisos 0600 en Linux y ese modo
        // viajaria al destino (el otro sistema, o un respaldo del servidor, no podria leerlo); asi el modo sale del umask, como
        // cualquier archivo que escribe la aplicacion.
        Path temporal = dir.resolve(prefijoTemporal + "-" + UUID.randomUUID() + ".tmp");
        boolean movido = false;
        try {
            long total = cabecera.length;
            try (OutputStream salida = Files.newOutputStream(temporal, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                salida.write(cabecera);
                byte[] buffer = new byte[8192];
                int n;
                while ((n = entrada.read(buffer)) >= 0) {
                    total += n;
                    if (total > TAMANO_MAXIMO_BYTES) {
                        throw muyGrande.apply(total);
                    }
                    salida.write(buffer, 0, n);
                }
            }
            try {
                Files.move(temporal, destino, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING);
            }
            movido = true;
            return total;
        } finally {
            if (!movido) {
                try {
                    Files.deleteIfExists(temporal);
                } catch (IOException e) {
                    log.warn("No se pudo borrar el temporal {} del PDF: {}", temporal.getFileName(), e.toString());
                }
            }
        }
    }

    /** La hora de modificacion del archivo en la zona del reloj, como {@code yyyy-MM-dd'T'HH:mm:ss}; falla con {@link NoSuchFileException} si ya no esta. */
    static String fechaDeModificacion(Path pdf, ZoneId zona) throws IOException {
        LocalDateTime fecha = LocalDateTime.ofInstant(Files.getLastModifiedTime(pdf).toInstant(), zona);
        return fecha.format(FECHA);
    }
}
